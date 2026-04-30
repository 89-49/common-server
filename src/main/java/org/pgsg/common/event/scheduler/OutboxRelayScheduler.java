package org.pgsg.common.event.scheduler;

import java.util.List;
import java.util.UUID;

import org.pgsg.common.domain.Outbox;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.domain.OutboxStatus;
import org.pgsg.common.domain.QOutbox;
import org.pgsg.common.event.OutboxMessageFactory;
import org.pgsg.common.event.OutboxService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final OutboxService outboxService;

	private static final int FETCH_SIZE = 500;

	@Scheduled(fixedDelay = 5000)
	public void resendFailedMessages() {
		List<Outbox> retryableMessages = fetchRetryableMessages();

		if (retryableMessages.isEmpty()) {
			return;
		}

		log.info("재전송 대상 {}건 발견. 처리를 시작합니다.", retryableMessages.size());
		retryableMessages.forEach(this::processMessage);
	}

	private List<Outbox> fetchRetryableMessages() {
		return outboxRepository.findAll(
				QOutbox.outbox.status.in(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED))
						.and(QOutbox.outbox.retryCount.lt(Outbox.MAX_RETRY_COUNT)),
				QOutbox.outbox.createdAt.asc(),
				FETCH_SIZE
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
