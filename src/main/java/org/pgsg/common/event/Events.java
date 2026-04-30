package org.pgsg.common.event;

import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class Events implements ApplicationContextAware {

	private static ApplicationEventPublisher eventPublisher;

	@Override
	public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
		Events.eventPublisher = applicationContext;
	}

	public static void trigger(Object event) {
		Objects.requireNonNull(eventPublisher, "Events가 초기화되지 않았습니다");
		eventPublisher.publishEvent(event);
	}
}
