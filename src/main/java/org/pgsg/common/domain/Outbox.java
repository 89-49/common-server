package org.pgsg.common.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Getter
@Builder
@Access(AccessType.FIELD)
@Table(name = "p_outbox",
	indexes = {
	@Index(name = "idx_outbox_status", columnList = "status")}	//status를 기준으로 index 생성
	,uniqueConstraints = {
	@UniqueConstraint(name="uk_outbox_correlation_id_type", columnNames = {"correlationId", "eventType"})})	//멱등성 보장을 위해 correlationId와 eventType으로 복합키 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Outbox extends BaseEntity{
	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name="message_id")
	@GeneratedValue(strategy = GenerationType.UUID)
	protected UUID id;

	@Column(nullable = false)
	protected UUID correlationId; // 거래나 예약 등에 관여한 서비스 이벤트들을 하나로 연결 - 현재는 상품 id를 사용하는 것으로 생각중

	@Column(nullable = false)
	protected String domainType;	//도메인명 - product, trade 등

	@Column(nullable = false)
	protected UUID domainId;	//todo: kafka의 파티션 키 후보로 생각중

	@Column(nullable = false)
	protected String eventType; // 토픽명은 configs 참조 - ex) prod-trade-created

	@JdbcTypeCode(SqlTypes.JSON)
	protected String payload; // 전송한 메세지(JSON 형식)

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable=false)
	protected OutboxStatus status = OutboxStatus.PENDING;

	@Builder.Default
	protected int retryCount = 0; // 재시도 카운트

	public void complete() {
		this.status = OutboxStatus.PROCESSED;
	}

	public void fail() {
		if(retryCount>3)	//재시도 횟수 3회로 설정
			this.status = OutboxStatus.FAILED;
		else
			this.retryCount++;
	}
	public void permanent_fail() {
		this.status = OutboxStatus.PERMANENT_FAILURE;
	}

	public void backToReady(){
		this.status=OutboxStatus.PENDING;
	}

}
