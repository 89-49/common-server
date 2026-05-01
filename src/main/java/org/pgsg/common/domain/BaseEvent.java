package org.pgsg.common.domain;

import java.util.UUID;

public interface BaseEvent {
	UUID correlationId(); // 시스템 전반의 흐름 추적 (Tracing)
	UUID domainId();    // 대상 식별자 (Order ID 등)
	String domainType();	//도메인 명
	String eventType();	//이벤트 타입 - configs에 있는 토픽명, 예) prod-product-created
	Object payload();
}
