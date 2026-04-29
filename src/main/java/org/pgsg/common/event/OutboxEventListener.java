package org.pgsg.common.event;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.pgsg.common.domain.Outbox;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.domain.OutboxStatus;
import org.pgsg.common.domain.QOutbox;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

	@EventListener
	@Transactional(propagation = Propagation.REQUIRED)
	public void recordOutbox(OutboxEvent event) {

		if (outboxRepository.exists(QOutbox.outbox.correlationId.eq(event.correlationId()))) {
			log.warn("이미 존재하는 correlationId 입니다.: {}", event.correlationId());
			return;
		}

		try {
			String jsonPayload = objectMapper.writeValueAsString(event.payload());

			Outbox outbox = Outbox.builder()
				.correlationId(event.correlationId())
				.domainType(event.domainType())
				.domainId(event.domainId())
				.eventType(event.eventType())
				.payload(jsonPayload)
				.status(OutboxStatus.PENDING) // 메세지 전송전 단계는 PENDING, 전송 완료 후는 PROCESSED호 변경됨
				.build();
			outboxRepository.save(outbox);
		} catch (JsonProcessingException e) {
			log.error("Output payload 직렬화 실패: {}", event.correlationId(), e);
		}
	}

	// fallbackExecution = true, @Transactional이 없는 환경(단순 메시지 전송)에서도 실행 보장
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void publish(OutboxEvent event) {
		outboxRepository.findByCorrelationId(event.correlationId()).ifPresent(outbox -> {
			// Kafka 메시지에 ID 헤더 추가
			ProducerRecord<String, Object> record = new ProducerRecord<>(
				outbox.getEventType(),
				outbox.getDomainId(),
				outbox.getPayload()
			);
			record.headers().add("message_id", outbox.getId().toString().getBytes());

			kafkaTemplate.send(record)
				.whenComplete((result, e) -> {
					if (e == null) outboxService.handleSuccess(event.correlationId());
					else outboxService.handleFailure(event, e);
				});
		});
	}


}