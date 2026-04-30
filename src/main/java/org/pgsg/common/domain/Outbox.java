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
@Table(name = "p_outbox", indexes = {
	@Index(name = "idx_outbox_status", columnList = "status")})	//status를 기준으로 index 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Outbox extends BaseEntity{
	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name="message_id")
	@GeneratedValue(strategy = GenerationType.UUID)
	protected UUID id;

	@Column(nullable = false, unique = true)
	protected UUID correlationId; // 거래나 예약 등에 관여한 서비스 이벤트들을 하나로 연결

	@Column(nullable = false)
	protected String domainType;

	@Column(nullable = false)
	protected String domainId;	//todo: kafka의 파티션 키 후보로 생각중

	@Column(nullable = false)
	protected String eventType; // 이벤트 타입, 카프카를 쓰게되면 Topic이 될 것

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

}
