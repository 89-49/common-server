package org.pgsg.common.event;

import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.pgsg.common.domain.BaseEvent;
import org.pgsg.common.domain.Outbox;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.domain.OutboxStatus;
import org.pgsg.common.util.JsonUtil;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {
	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private static final int MAX_RETRY_COUNT = 3; // 재시도 최대 횟수

	@Lazy
	private final OutboxService self;
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleSuccess(UUID id) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			outbox.complete();
			outboxRepository.save(outbox);
			log.info("Outbox 메세지 전송 및 상태 완료 변경 성공: {}", id);
		});

	}

	//최대 재시도 이상 실패 시 dlt 전송 -> dlt 전송 실패는 로그만 남겨서 직접 처리하도록 설계
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleFailure(UUID id, Throwable e) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			outbox.fail();

			if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
				log.error("최대 재시도 횟수 초과(Total: {}). DLT로 격리합니다: {}", outbox.getRetryCount(), id);
				outbox.permanent_fail();
				outboxRepository.saveAndFlush(outbox);
				self.sendToDlt(id);
			} else {
				outbox.backToReady();
				outboxRepository.saveAndFlush(outbox);
				log.warn("메세지 전송 실패 (재시도 예정 {}/{}): {}", outbox.getRetryCount(), MAX_RETRY_COUNT, id);
			}
		});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean claimForSending(UUID id) {
		int updatedRows = outboxRepository.updateStatusIfReady(id, OutboxStatus.SENDING);
		return updatedRows > 0;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendToDlt(UUID targetId) {
		Outbox outbox = outboxRepository.findById(targetId)
			.orElseThrow(() -> new EntityNotFoundException("Outbox not found: " + targetId));

		try {
			String dltTopic = outbox.getEventType() + ".dlt";
			ProducerRecord<String, Object> record = new ProducerRecord<>(
				dltTopic,
				outbox.getDomainId().toString(),
				outbox.getPayload()
			);

			record.headers().add("message_id", outbox.getId().toString().getBytes());
			record.headers().add("correlation_id", outbox.getCorrelationId().toString().getBytes());
			record.headers().add("error_reason", "MAX_RETRY_EXCEEDED".getBytes());

			kafkaTemplate.send(record).whenComplete((result, ex)-> {
				if(ex==null){
					self.finalizePermanentFailure(targetId);
					log.info("DLT 전송 및 상태 변경 완료: {}", targetId);
				}else{
					log.error("DLT 전송 실패 (상태 유지): {}", targetId, ex);
				}
			});
		} catch (Exception e) {
			log.error("DLT 전송 중 예외 발생: {}", targetId, e);
		}
	}

	@Transactional
	public Outbox saveEvent(BaseEvent event) {
		String eventType = event.getClass().getSimpleName();	//todo: 확인 후 필요 시 수정
		UUID domainId = event.domainId(); // 도메인 ID 추출 헬퍼 (별도 구현 필요)
		UUID correlationId = event.correlationId(); // 흐름 추적 ID 추출

		// 2. Outbox 엔티티 생성 (PENDING 상태로 시작)
		try {
			String jsonPayload = JsonUtil.toJson(event.payload());

			Outbox outbox = Outbox.builder()
				.eventType(eventType)
				.domainId(domainId)
				.correlationId(correlationId)
				.payload(jsonPayload)
				.status(OutboxStatus.PENDING)
				.retryCount(0)
				.build();

			log.info("Outbox 이벤트 저장 완료 - ID: {}, Type: {}", outbox.getId(), eventType);
			return outboxRepository.save(outbox);
		} catch (Exception e) {
			log.error("Output payload 직렬화 실패: {}", event.correlationId(), e);
			throw new RuntimeException("이벤트 직렬화 실패로 인한 Outbox 등록 중단", e);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finalizePermanentFailure(UUID id) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			outbox.permanent_fail();
			outboxRepository.saveAndFlush(outbox);
		});
	}
}
