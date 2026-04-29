package org.pgsg.common.event;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;


public class Events {
	private static KafkaTemplate<String, Object> kafkaTemplate;
	private static ApplicationEventPublisher eventPublisher;

	@Autowired
	public void init(KafkaTemplate<String, Object> kafkaTemplate, ApplicationEventPublisher eventPublisher) {
		Events.kafkaTemplate = kafkaTemplate;
		Events.eventPublisher = eventPublisher;
	}

	public static void trigger(String correlationId, String domainType, String domainId, String eventType, Object payload) {
		if (kafkaTemplate != null && eventPublisher != null) {
			eventPublisher.publishEvent(new OutboxEvent(correlationId, domainType, domainId, eventType, payload));
		}
	}
}