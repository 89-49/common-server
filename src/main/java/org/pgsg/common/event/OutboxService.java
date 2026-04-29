package org.pgsg.common.event;

import java.util.UUID;

import org.pgsg.common.domain.OutboxRepository;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {
	private final OutboxRepository outboxRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	private static final int MAX_RETRY_COUNT = 3; // 재시도 최대 횟수

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void updateStatus(UUID id, boolean isSuccess) {
		outboxRepository.findById(id).ifPresent(outbox -> {
			if (isSuccess) {
				outbox.complete();
				log.info("재전송 성공: {}", outbox.getCorrelationId());
			}
			else {
				outbox.fail();
				log.warn("재전송 실패 (현재 횟수: {}): {}", outbox.getRetryCount(), outbox.getCorrelationId());
			}
			outboxRepository.saveAndFlush(outbox);
		});
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleSuccess(String correlationId) {
		outboxRepository.findByCorrelationId(correlationId).ifPresent(outbox -> {
			outbox.complete();
			outboxRepository.save(outbox);
			log.info("Outbox 메세지 전송 및 상태 완료 변경 성공: {}", correlationId);
		});

	}

	//최대 재시도 이상 실패 시 dlt 전송 -> dlt 전송 실패는 로그만 남겨서 직접 처리하도록 설계
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void handleFailure(OutboxEvent event, Throwable e) {
		outboxRepository.findByCorrelationId(event.correlationId()).ifPresent(outbox -> {
			outbox.fail();
			outboxRepository.saveAndFlush(outbox); // 횟수와 FAILED 상태 즉시 반영

			if (outbox.getRetryCount() >= MAX_RETRY_COUNT) {
				log.error("최대 재시도 횟수 초과(Total: {}). DLT로 격리합니다: {}", outbox.getRetryCount(), event.correlationId());
				sendToDlt(event, outbox.getPayload());
			} else {
				log.warn("메세지 전송 실패 (재시도 예정 {}/{}): {}", outbox.getRetryCount(), MAX_RETRY_COUNT, event.correlationId());
			}
		});
	}

	// DLT 전송
	private void sendToDlt(OutboxEvent event, String payload) {
		String dltTopic = event.eventType() + ".DLT";
		try {
			// DLT 전송은 재시도 없이 1회만 시도, 실패 시 에러 로그만 기록
			kafkaTemplate.send(dltTopic, event.domainId(), payload)
				.whenComplete((res, e) -> {
					if (e != null) log.error("DLT 전송 실패: {}", event.correlationId(), e);
					else log.info("DLT 전송 성공: {}", event.correlationId());
				});
		} catch (Exception e) {
			log.error("DLT 전송 중 예외 발생: {}", event.correlationId(), e);
		}
	}
}
