package org.pgsg.common.domain;

import java.util.UUID;

public interface BaseEvent {
	UUID correlationId(); // 시스템 전반의 흐름 추적 (Tracing)
	UUID domainId();    // 대상 식별자 (Order ID 등)
	Object payload();
}
