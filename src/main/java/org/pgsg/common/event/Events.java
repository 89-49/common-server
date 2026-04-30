package org.pgsg.common.event;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Events {
	private static KafkaTemplate<String, Object> kafkaTemplate;
	private static ApplicationEventPublisher eventPublisher;

	@Autowired
	public void init(KafkaTemplate<String, Object> kafkaTemplate, ApplicationEventPublisher eventPublisher) {
		Events.kafkaTemplate = kafkaTemplate;
		Events.eventPublisher = eventPublisher;
	}

	public static void trigger(Object event) {
		if (kafkaTemplate == null && eventPublisher == null) {
			String errorMsg = "Events class has not been initialized. Call Events.init(ApplicationEventPublisher, KafkaTemplate) during application startup.";
			log.error(errorMsg);
			throw new IllegalStateException(errorMsg);
		}

		eventPublisher.publishEvent(event);
	}
}