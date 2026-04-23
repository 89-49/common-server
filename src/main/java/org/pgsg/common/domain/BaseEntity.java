package org.pgsg.common.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.pgsg.common.util.SecurityUtil;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@CreatedBy
	@Column(updatable = false)
	protected UUID createdBy;

	@CreatedDate
	@Column(updatable = false)
	protected LocalDateTime createdAt;

	@LastModifiedBy
	@Column(insertable = false)
	protected UUID modifiedBy;

	@LastModifiedDate
	@Column(insertable = false)
	protected LocalDateTime modifiedAt;

	@Column(insertable = false)
	protected UUID deletedBy;

	@Column
	protected LocalDateTime deletedAt;

	protected void delete(UUID deletedBy) {
		// 이미 삭제된 경우 중복 처리 방지
		this.deletedBy = deletedBy!=null
			? deletedBy
			: SecurityUtil.getCurrentUserId().orElse(UUID.fromString("00000000-0000-0000-0000-00000000000"));

		this.deletedAt = LocalDateTime.now();
	}
}
