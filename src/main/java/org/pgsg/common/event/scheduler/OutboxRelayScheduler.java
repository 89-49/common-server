package org.pgsg.common.event.scheduler;

import java.util.List;
import java.util.UUID;

import org.pgsg.common.domain.Outbox;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.domain.OutboxStatus;
import org.pgsg.common.domain.QOutbox;
import org.pgsg.common.event.OutboxService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OutboxRelayScheduler {
	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final OutboxService outboxService;

	private final int MAX_RETRY_COUNT = 3;	//todo: 추후 조정 가능

	@Transactional
	@Scheduled(fixedDelay = 5000) //todo: 성능 확인 후 cdc 툴 사용 고려
	public void resendFailedMessages() {
		// PENDING, FAILED 상태이면서 retryCount가 5 미만 목록 조회
		List<Outbox> items = (List<Outbox>)outboxRepository.findAll(QOutbox.outbox.status.in(List.of(OutboxStatus.PENDING, OutboxStatus.FAILED))
			.and(QOutbox.outbox.retryCount.lt(MAX_RETRY_COUNT)),
		QOutbox.outbox.createdAt.asc());

		if (items.isEmpty()) return;

		log.info("재전송 대상 {}건 발견. 처리를 시작합니다.", items.size());

		for (Outbox outbox : items) {
			UUID targetId = outbox.getId();
			try {
				kafkaTemplate.send(outbox.getEventType(), outbox.getDomainId(), outbox.getPayload())
					.whenComplete((result, e) -> outboxService.updateStatus(targetId, e == null));
			} catch (Exception e) {
				outboxService.updateStatus(targetId, false);
				log.error("재전송 중 예외 발생: {}", outbox.getCorrelationId(), e);
			}
		}
	}
}
