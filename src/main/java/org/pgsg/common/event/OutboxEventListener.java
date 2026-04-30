package org.pgsg.common.event;

import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.pgsg.common.domain.BaseEvent;
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
			outboxService.saveEvent(event);
		} catch (DataIntegrityViolationException e) {
			if(isUniqueConstraintViolation(e,"uk_outbox_correlation_id"))
				log.warn("이미 처리 중인 중복 Outbox 이벤트입니다. correlationId: {}", event.correlationId());
			else
				throw e;
		}
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void publish(OutboxEvent event) {
		List<Outbox> outboxes=outboxRepository.findAllByCorrelationId(event.correlationId());

			for(Outbox outbox:outboxes){
				if(outbox.getStatus().equals(OutboxStatus.PENDING)){
					UUID targetId=outbox.getId();

					if(outboxService.claimForSending(targetId)){
						sendToKafka(outbox,targetId);
					}else{	//점유 실패 시 스케줄러 등 다른 프로세스가 처리 중인 것으로 간주하고 스킵
						log.debug("이미 처리 중이거나 점유된 메시지입니다. 발행을 건너뜁니다: {}", targetId);
					}

				}
			}
	}


	private boolean isUniqueConstraintViolation(DataIntegrityViolationException ex, String constraintName) {
		Throwable cause = ex.getMostSpecificCause();
		if (cause instanceof java.sql.SQLException sqlEx) {
			String sqlState = sqlEx.getSQLState();
			// Postgres: 23505 (unique_violation), MySQL: 1062 (Duplicate entry)
			if ("23505".equals(sqlState) || sqlEx.getErrorCode() == 1062) {
				return sqlEx.getMessage().contains(constraintName);
			}
		}
		return false;
	}

	private void sendToKafka(Outbox outbox, UUID targetId) {
		try {
			ProducerRecord<String, Object> record = new ProducerRecord<>(
				outbox.getEventType(),
				outbox.getDomainId().toString(),
				outbox.getPayload()
			);

			record.headers().add("message_id", targetId.toString().getBytes());
			record.headers().add("correlation_id", outbox.getCorrelationId().toString().getBytes());

			kafkaTemplate.send(record)
				.whenComplete((result, e) -> {
					if (e == null) {
						outboxService.handleSuccess(targetId);
					} else {
						outboxService.handleFailure(targetId, e);
					}
				});
		} catch (Exception e) {
			// 전송 준비 단계(Serialization 등) 예외 방어
			outboxService.handleFailure(targetId, e);
			log.error("Kafka 전송 준비 중 예외 발생 - ID: {}", targetId, e);
		}
	}

	//비트랜잭션 처리용 - 명시적 호출 필요
	public void immediatePublish(BaseEvent event) {
		log.info("비트랜잭션 환경 즉시 발행 시작");
		Outbox outbox = outboxService.saveEvent(event); // 직접 저장
		if (outboxService.claimForSending(outbox.getId())) {
			sendToKafka(outbox, outbox.getId());
		}
	}
}