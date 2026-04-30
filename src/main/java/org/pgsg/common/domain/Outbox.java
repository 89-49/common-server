package org.pgsg.common.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@Access(AccessType.FIELD)
@Table(name = "p_outbox", indexes = {
		@Index(name = "idx_outbox_status", columnList = "status"),
		@Index(name = "idx_outbox_correlation_id", columnList = "correlationId")})    //status, correlationId 기준 index 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Outbox extends BaseEntity{

	public static final int MAX_RETRY_COUNT = 3;
	private static final String DLT_TOPIC_SUFFIX = "-dlt";

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name="message_id")
	@GeneratedValue(strategy = GenerationType.UUID)
	protected UUID id;

	@Column(nullable = false)
	protected UUID correlationId; // 거래나 예약 등에 관여한 서비스 이벤트들을 하나로 연결

	@Column(nullable = false)
	protected String domainType;

	// TODO: kafka의 파티션 키 후보로 domainId를 활용할 수 있을지 검토 필요
	@Column(nullable = false)
	protected UUID domainId;

	@Column(nullable = false)
	protected String eventType; // topic name

	@JdbcTypeCode(SqlTypes.JSON)
	protected String payload;

	@Builder.Default
	@Enumerated(EnumType.STRING)
	@Column(nullable=false)
	protected OutboxStatus status = OutboxStatus.PENDING;

	@Builder.Default
	protected int retryCount = 0;

	// SENDING 상태로 점유한 시각
	@Column
	protected LocalDateTime claimedAt;

	public void complete() {
		this.status = OutboxStatus.PROCESSED;
	}

	public void fail() {
		this.retryCount++;
		if (this.retryCount >= MAX_RETRY_COUNT) {
			this.status = OutboxStatus.FAILED;
		} else {
			this.status = OutboxStatus.PENDING;
		}
	}

	public void permanentFail() {
		this.status = OutboxStatus.PERMANENT_FAILURE;
	}

	public boolean isFailed() {
		return this.status == OutboxStatus.FAILED;
	}

	public String getDltTopic() {
		return this.eventType + DLT_TOPIC_SUFFIX;
	}
}
