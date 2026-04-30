package org.pgsg.common.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.pgsg.common.alert.AlertNotifier;
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

	private static final String DLT_REASON_MAX_RETRY = "MAX_RETRY_EXCEEDED";

	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;
	private final AlertNotifier alertNotifier;

	@Lazy
	private final OutboxService self;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleSuccess(UUID id) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			outbox.complete();
			outboxRepository.saveAndFlush(outbox);
			log.info("Outbox 메세지 전송 및 상태 완료 변경 성공: {}", id);
		});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleFailure(UUID id, Throwable e) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			outbox.fail();
			outboxRepository.saveAndFlush(outbox);

			if (outbox.isFailed()) {
				log.error("최대 재시도 횟수 초과(Total: {}). DLT로 격리합니다: {}", outbox.getRetryCount(), id, e);
				self.sendToDlt(id);
			} else {
				log.warn("메세지 전송 실패 (재시도 예정 {}/{}): {}", outbox.getRetryCount(), Outbox.MAX_RETRY_COUNT, id, e);
			}
		});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean claimForSending(UUID id) {
		int updatedRows = outboxRepository.updateStatusIfReady(id, OutboxStatus.SENDING, LocalDateTime.now());
		return updatedRows > 0;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void sendToDlt(UUID targetId) {
		Outbox outbox = outboxRepository.findById(targetId)
				.orElseThrow(() -> new EntityNotFoundException("Outbox not found: " + targetId));

		try {
			ProducerRecord<String, Object> record =
					OutboxMessageFactory.toDltRecord(outbox, DLT_REASON_MAX_RETRY);

			kafkaTemplate.send(record).whenComplete((result, ex) -> {
				if (ex == null) {
					self.finalizePermanentFailure(targetId);
					log.info("DLT 전송 및 상태 변경 완료: {}", targetId);
				} else {
					log.error("DLT 전송 실패 (상태 유지): {}", targetId, ex);
					alertNotifier.notifyDltFailure(targetId, outbox.getEventType(), ex);
				}
			});
		} catch (Exception e) {
			log.error("DLT 전송 중 예외 발생: {}", targetId, e);
			alertNotifier.notifyDltFailure(targetId, outbox.getEventType(), e);
		}
	}

	@Transactional
	public Outbox saveEvent(BaseEvent event) {
		String eventType = event.getClass().getSimpleName();   // TODO: 확인 후 필요 시 수정
		UUID domainId = event.domainId();
		UUID correlationId = event.correlationId();
		String domainType = event.domainType();

		String jsonPayload;
		try {
			jsonPayload = JsonUtil.toJson(event.payload());
		} catch (Exception e) {
			log.error("Output payload 직렬화 실패: {}", correlationId, e);
			throw new RuntimeException("이벤트 직렬화 실패로 인한 Outbox 등록 중단", e);
		}

		Outbox outbox = Outbox.builder()
				.eventType(eventType)
				.domainType(domainType)
				.domainId(domainId)
				.correlationId(correlationId)
				.payload(jsonPayload)
				.build();

		Outbox saved = outboxRepository.save(outbox);
		log.info("Outbox 이벤트 저장 완료 - ID: {}, Type: {}", saved.getId(), eventType);
		return saved;
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void finalizePermanentFailure(UUID id) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			outbox.permanentFail();
			outboxRepository.saveAndFlush(outbox);
		});
	}
}
