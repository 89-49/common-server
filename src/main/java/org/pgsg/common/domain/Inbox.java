package org.pgsg.common.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@Access(AccessType.FIELD)
@Table(name = "p_inbox", indexes = {
	@Index(name = "idx_inbox_message_group", columnList = "messageGroup"),	//메시지 그룹으로 인덱스 생성
	@Index(name = "idx_inbox_processed_at", columnList = "receivedAt")		//처리일로 인덱스 생성
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class Inbox {
	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name="message_id")
	protected UUID id; // Outbox에 등록된 메세지 ID와 동일하게 유지

	@Column
	protected String messageGroup;	//co

	@Builder.Default
	@Column
	protected InboxStatus status=InboxStatus.RECEIVED;

	@CreatedDate
	@Column(updatable=false)
	protected LocalDateTime receivedAt;


	public void complete() {
		this.status = InboxStatus.PROCESSED;
		this.receivedAt = LocalDateTime.now();
	}

	public void fail() {
		this.status = InboxStatus.FAILED;
	}
}
