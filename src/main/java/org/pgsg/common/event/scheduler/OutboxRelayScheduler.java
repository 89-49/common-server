package org.pgsg.common.event.scheduler;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.pgsg.common.domain.Outbox;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.domain.OutboxStatus;
import org.pgsg.common.domain.QOutbox;
import org.pgsg.common.event.OutboxMessageFactory;
import org.pgsg.common.event.OutboxService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelayScheduler {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final OutboxService outboxService;

	private static final int FETCH_SIZE = 500;
	private static final int STUCK_TIMEOUT_MINUTES = 5;

	@Scheduled(fixedDelay = 5000)	// 5초
	public void resendFailedMessages() {
		List<Outbox> retryableMessages = fetchRetryableMessages();

		if (retryableMessages.isEmpty()) {
			return;
		}

		log.info("재전송 대상 {}건 발견. 처리를 시작합니다.", retryableMessages.size());
		retryableMessages.forEach(this::processMessage);
	}

	@Transactional
	@Scheduled(fixedDelay = 60_000)	// 1분
	@SchedulerLock(name = "outbox-relay-reclaim", lockAtMostFor = "PT2M", lockAtLeastFor = "PT5S")
	public void reclaimStuckSendingMessages() {
		LocalDateTime threshold = LocalDateTime.now().minusMinutes(STUCK_TIMEOUT_MINUTES);
		int reclaimed = outboxRepository.reclaimStuckSending(threshold);
		if (reclaimed > 0) {
			log.warn("SENDING 상태로 {}분 이상 정체된 메시지 {}건을 PENDING으로 복구함",
					STUCK_TIMEOUT_MINUTES, reclaimed);
		}
	}

	private List<Outbox> fetchRetryableMessages() {
		return outboxRepository.findRetryableMessages(
				List.of(OutboxStatus.PENDING, OutboxStatus.FAILED),
				Outbox.MAX_RETRY_COUNT,
				PageRequest.of(0, FETCH_SIZE)
		);
	}

	private void processMessage(Outbox outbox) {
		UUID targetId = outbox.getId();

		if (!outboxService.claimForSending(targetId)) {
			return;
		}

		try {
			sendToKafka(outbox);
		} catch (Exception e) {
			outboxService.handleFailure(targetId, e);
			log.error("재전송 중 예외 발생: {}", targetId, e);
		}
	}

	private void sendToKafka(Outbox outbox) {
		UUID targetId = outbox.getId();

		kafkaTemplate.send(OutboxMessageFactory.toRecord(outbox))
				.whenComplete((result, e) -> {
					if (e == null) {
						outboxService.handleSuccess(targetId);
					} else {
						outboxService.handleFailure(targetId, e);
					}
				});
	}
}
