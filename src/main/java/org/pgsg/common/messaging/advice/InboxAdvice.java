package org.pgsg.common.messaging.advice;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.pgsg.common.domain.Inbox;
import org.pgsg.common.domain.InboxRepository;
import org.pgsg.common.domain.InboxStatus;
import org.pgsg.common.messaging.annotation.IdempotentConsumer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.Message;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Aspect
@RequiredArgsConstructor
public class InboxAdvice {
	private final InboxRepository inboxRepository;

	@Around("@annotation(idempotentConsumer)")
	@Transactional(rollbackFor = Exception.class)
	public Object handle(ProceedingJoinPoint joinPoint, IdempotentConsumer idempotentConsumer) throws Throwable {
		UUID messageId = extractMessageId(joinPoint.getArgs());

		if (messageId == null) {
			log.warn("Message ID가 없는 메시지입니다. 멱등성 체크를 진행하지 않습니다.");
			return joinPoint.proceed();
		}

		Optional<Inbox> existingInbox = inboxRepository.findById(messageId);
		if (existingInbox.isPresent() && existingInbox.get().getStatus() == InboxStatus.PROCESSED) {
			log.info("이미 처리가 완료된 메시지입니다. (ID: {})", messageId);
			return null;
		}

		Inbox inbox;
		if(existingInbox.isEmpty()) {
			try {
				// 메서드 실행 성공 시 Inbox에 기록 (messageId는 Outbox에서 발행된 ID이며 동일하게 저장합니다.)
				inbox = Inbox.builder()
					.id(messageId)
					.messageGroup(idempotentConsumer.value())
					.build();

				inboxRepository.saveAndFlush(inbox);

			} catch (DataIntegrityViolationException e) {
				log.info("동시 유입된 중복 메시지입니다: (ID: {})", messageId);
				return null;
			}
		}
		else{
			inbox = existingInbox.get();
		}

		try {
			Object result =  joinPoint.proceed();
			inbox.complete();
			inboxRepository.save(inbox);
			log.debug("메시지 처리 완료: {}", messageId);

			return result;
		} catch (Throwable throwable) {
			log.error("메세지 처리 실패, Inbox 기록을 롤백합니다: {}", messageId);
			throw throwable;
		}
	}

	private UUID extractMessageId(Object[] args) {
		for (Object arg : args) {
			// Kafka 리스너에서 ConsumerRecord<K, V>를 파라미터로 받을 경우
			if (arg instanceof ConsumerRecord<?, ?> record) {
				Header header = record.headers().lastHeader("message_id");
				if (header != null) return parseUuid(header.value());
			}

			// Spring Messaging Message 객체인 경우
			if (arg instanceof Message<?> message) {
				Object header = message.getHeaders().get("message_id");
				if (header instanceof byte[] bytes) return parseUuid(bytes);
				if (header instanceof String str) return UUID.fromString(str);
			}
		}
		return null;
	}

	private UUID parseUuid(byte[] value) {
		try {
			return UUID.fromString(new String(value, StandardCharsets.UTF_8));
		} catch (Exception e) {
			log.error("메시지 ID 형식이 유효하지 않습니다:{}", value, e);
			return null;
		}
	}
}
