package org.pgsg.common.event;

import java.util.UUID;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.pgsg.common.domain.Outbox;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.domain.OutboxStatus;
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
	public void handleSuccess(UUID correlationId) {
		outboxRepository.findByCorrelationId(correlationId).ifPresent(outbox -> {
			outbox.complete();
			outboxRepository.save(outbox);
			log.info("Outbox 메세지 전송 및 상태 완료 변경 성공: {}", correlationId);
		});

	}

	//최대 재시도 이상 실패 시 dlt 전송 -> dlt 전송 실패는 로그만 남겨서 직접 처리하도록 설계
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleFailure(UUID id, Throwable e) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			outbox.fail();
			outboxRepository.saveAndFlush(outbox); // 횟수와 FAILED 상태 즉시 반영

			if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
				log.error("최대 재시도 횟수 초과(Total: {}). DLT로 격리합니다: {}", outbox.getRetryCount(), id);
				self.sendToDlt(id);
			} else {
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
				outbox.getDomainId(),
				outbox.getPayload()
			);

			record.headers().add("message_id", outbox.getId().toString().getBytes());
			record.headers().add("correlation_id", outbox.getCorrelationId().toString().getBytes());
			record.headers().add("error_reason", "MAX_RETRY_EXCEEDED".getBytes());

			kafkaTemplate.send(record);
			log.info("DLT 전송 시도 완료: {}", targetId);
		} catch (Exception e) {
			log.error("DLT 전송 중 예외 발생: {}", targetId, e);
		} finally {
			// 최종 실패 상태로 변경하여 스케줄러 대상에서 제외
			outbox.permanent_fail();
			outboxRepository.saveAndFlush(outbox);
		}
	}
}
