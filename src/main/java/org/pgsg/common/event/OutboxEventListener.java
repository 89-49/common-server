package org.pgsg.common.event;

import java.util.List;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
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
			String jsonPayload = objectMapper.writeValueAsString(event.payload());

			Outbox outbox = Outbox.builder()
				.correlationId(event.correlationId())
				.domainType(event.domainType())
				.domainId(event.domainId())
				.eventType(event.eventType())
				.payload(jsonPayload)
				.status(OutboxStatus.PENDING) // 메세지 전송전 단계는 PENDING, 전송 완료 후는 PROCESSED로 변경됨
				.build();

			outboxRepository.saveAndFlush(outbox);
		} catch (DataIntegrityViolationException e) {
			log.warn("이미 처리 중인 중복 Outbox 이벤트입니다. correlationId: {}", event.correlationId());
		} catch (JsonProcessingException e) {
			log.error("Output payload 직렬화 실패: {}", event.correlationId(), e);
			throw new RuntimeException("이벤트 직렬화 실패로 인한 Outbox 등록 중단", e);
		}
	}

	// fallbackExecution = true, @Transactional이 없는 환경(단순 메시지 전송)에서도 실행 보장
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
	public void publish(OutboxEvent event) {
		List<Outbox> outboxes=outboxRepository.findAllByCorrelationId(event.correlationId());

			for(Outbox outbox:outboxes){
				if(outbox.getStatus().equals(OutboxStatus.PENDING)){

					UUID targetId=outbox.getId();

					if(outboxService.claimForSending(targetId)){
						try {
							ProducerRecord<String, Object> record = new ProducerRecord<>(
								outbox.getEventType(),
								outbox.getDomainId(),
								outbox.getPayload()
							);

							record.headers().add("message_id", targetId.toString().getBytes());
							record.headers()
								.add("correlation_id", outbox.getCorrelationId().toString().getBytes());    //흐름 추적용

							kafkaTemplate.send(record)
								.whenComplete((result, e) -> {
									if (e == null)
										outboxService.handleSuccess(targetId);
									else
										outboxService.handleFailure(targetId, e);
								});
						}catch (Exception e) {	//전송 준비 단계 예외 발생 시
							outboxService.handleFailure(targetId, e);
							log.error("이벤트 즉시 발행 중 예외 발생: {}", targetId, e);
						}
					}else{	//점유 실패 시 스케줄러 등 다른 프로세스가 처리 중인 것으로 간주하고 스킵
						log.debug("이미 처리 중이거나 점유된 메시지입니다. 발행을 건너뜁니다: {}", targetId);
					}

				}
			}
	}


}