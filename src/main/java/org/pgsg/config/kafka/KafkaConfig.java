package org.pgsg.config.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;

@Configuration
@ConditionalOnClass(name = "org.apache.kafka.clients.producer.KafkaProducer")
public class KafkaConfig {
	@Bean
	public CommonErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
		// 실패한 메시지를 {원래토픽}.DLT 로 전송하는 리커버러 생성
		DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(template);

		// 에러 핸들러 등록 (yml에 정의한 back-off 설정을 자동으로 적용)
		return new DefaultErrorHandler(recoverer);
	}
}