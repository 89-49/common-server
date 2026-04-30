package org.pgsg.config.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.pgsg.common.domain.InboxRepository;
import org.pgsg.common.domain.OutboxRepository;
import org.pgsg.common.event.Events;
import org.pgsg.common.event.OutboxEventListener;
import org.pgsg.common.event.OutboxService;
import org.pgsg.common.event.scheduler.OutboxRelayScheduler;
import org.pgsg.common.messaging.advice.InboxAdvice;
import org.pgsg.common.messaging.processor.InboxCleanupProcessor;
import org.pgsg.common.messaging.scheduler.InboxCleanupScheduler;
import org.pgsg.common.util.MdcTaskDecorator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

import java.util.concurrent.Executor;

@EnableAsync
@Configuration
@EnableScheduling
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class EventConfig implements AsyncConfigurer {

	@Bean
	public Events events() {
		return new Events();
	}

	// 비동기 적용시 생성될 스레드 풀 설정
	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);        // 기본 스레드 수
		executor.setMaxPoolSize(50);        // 최대 스레드 수
		executor.setQueueCapacity(100);     // 대기 큐 용량
		executor.setThreadNamePrefix("Async-"); // 스레드 이름 접두사

		// MdcTaskDecorator 등록
		executor.setTaskDecorator(new MdcTaskDecorator());

		// 서버가 배포되거나 종료될 때 쓰레드가 갑자기 죽지 않도록 다음 설정 추가
		executor.setWaitForTasksToCompleteOnShutdown(true);
		executor.setAwaitTerminationSeconds(60); // 최대 60초 대기

		executor.initialize();
		return new DelegatingSecurityContextAsyncTaskExecutor(executor);
	}

	@Bean
	public OutboxEventListener outboxEventListener(OutboxRepository outboxRepository, KafkaTemplate<String, Object> kafkaTemplate, ObjectMapper objectMapper, OutboxService outboxService) {
		return new OutboxEventListener(outboxRepository, kafkaTemplate, objectMapper, outboxService);
	}

	@Bean
	public OutboxRelayScheduler OutboxRelayScheduler(OutboxRepository outboxRepository, KafkaTemplate<String, Object> kafkaTemplate, OutboxService outboxService) {
		return new OutboxRelayScheduler(outboxRepository, kafkaTemplate, outboxService);
	}

	@Bean
	public InboxAdvice inboxAdvice(InboxRepository inboxRepository) {
		return new InboxAdvice(inboxRepository);
	}

	@Bean
	public InboxCleanupProcessor inboxCleanupProcessor(InboxRepository inboxRepository) {
		return new InboxCleanupProcessor(inboxRepository);
	}

	@Bean
	public InboxCleanupScheduler inboxCleanupScheduler(InboxCleanupProcessor inboxCleanupProcessor) {
		return new InboxCleanupScheduler(inboxCleanupProcessor);
	}
}
