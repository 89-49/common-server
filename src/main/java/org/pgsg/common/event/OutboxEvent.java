package org.pgsg.common.event;

import java.util.Objects;
import java.util.UUID;

import org.pgsg.common.domain.BaseEvent;

public record OutboxEvent(
		UUID correlationId,
		String domainType,
		UUID domainId,
		String eventType,
		Object payload
) implements BaseEvent {

	public OutboxEvent {
		Objects.requireNonNull(correlationId, "correlationId must not be null");
		Objects.requireNonNull(domainType, "domainType must not be null");
		Objects.requireNonNull(domainId, "domainId must not be null");
		Objects.requireNonNull(eventType, "eventType must not be null");
		Objects.requireNonNull(payload, "payload must not be null");
	}
}
