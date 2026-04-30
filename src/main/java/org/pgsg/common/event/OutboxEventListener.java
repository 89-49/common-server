package org.pgsg.common.event;

import java.util.List;
import java.util.UUID;

import org.pgsg.common.domain.Outbox;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.domain.OutboxStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class OutboxEventListener {
	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final OutboxService outboxService;

	@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
	public void recordOutbox(OutboxEvent event) {
		try {
			String jsonPayload = serializePayload(event.payload());
			saveOutbox(event, jsonPayload);
		} catch (DataIntegrityViolationException e) {
			log.warn("이미 처리 중인 중복 Outbox 이벤트입니다. correlationId: {}", event.correlationId());
		} catch (JsonProcessingException e) {
			log.error("Output payload 직렬화 실패: {}", event.correlationId(), e);
			throw new RuntimeException("이벤트 직렬화 실패로 인한 Outbox 등록 중단", e);
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void publish(OutboxEvent event) {
		List<Outbox> outboxes = outboxRepository.findAllByCorrelationId(event.correlationId());

		outboxes.forEach(this::processImmediatePublish);
	}

	private void processImmediatePublish(Outbox outbox) {
		if (outbox.getStatus() != OutboxStatus.PENDING) {
			return;
		}

		UUID targetId = outbox.getId();
		if (!outboxService.claimForSending(targetId)) {
			log.debug("이미 처리 중이거나 점유된 메시지입니다. 발행을 건너뜁니다: {}", targetId);
			return;
		}

		sendToKafka(outbox);
	}

	private void sendToKafka(Outbox outbox) {
		UUID targetId = outbox.getId();
		try {
			kafkaTemplate.send(OutboxMessageFactory.toRecord(outbox))
					.whenComplete((result, e) -> {
						if (e == null) {
							outboxService.handleSuccess(targetId);
						} else {
							outboxService.handleFailure(targetId, e);
						}
					});
		} catch (Exception e) {
			outboxService.handleFailure(targetId, e);
			log.error("이벤트 즉시 발행 중 예외 발생: {}", targetId, e);
		}
	}

	private String serializePayload(Object payload) throws JsonProcessingException {
		return objectMapper.writeValueAsString(payload);
	}

	private void saveOutbox(OutboxEvent event, String jsonPayload) {
		Outbox outbox = Outbox.builder()
				.correlationId(event.correlationId())
				.domainType(event.domainType())
				.domainId(event.domainId())
				.eventType(event.eventType())
				.payload(jsonPayload)
				.status(OutboxStatus.PENDING)
				.build();

		outboxRepository.saveAndFlush(outbox);
	}
}
