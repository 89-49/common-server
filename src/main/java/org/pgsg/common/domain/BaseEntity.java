package org.pgsg.common.domain;

import java.time.LocalDateTime;

import org.pgsg.common.util.SecurityUtil;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.StringUtils;

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
	@Column(length=45, updatable = false)
	protected String createdBy;

	@CreatedDate
	@Column(updatable = false)
	protected LocalDateTime createdAt;

	@LastModifiedBy
	@Column(length=45, insertable = false)
	protected String modifiedBy;

	@LastModifiedDate
	@Column(insertable = false)
	protected LocalDateTime modifiedAt;

	@Column(length=45, insertable = false)
	protected String deletedBy;

	protected LocalDateTime deletedAt;

	protected void delete(String deletedBy) {
		// 이미 삭제된 경우 중복 처리 방지
		if (this.deletedAt != null) {
			return;
		}

		this.deletedBy = StringUtils.hasText(deletedBy)
			? deletedBy
			: SecurityUtil.getCurrentUsername().orElse("SYSTEM");	//todo: securityUtil 완성 후 검토

		this.deletedAt = LocalDateTime.now();
	}
}
