package org.pgsg.common.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

	@Modifying
	@Query("UPDATE Outbox o SET o.status = :outboxStatus, o.claimedAt = :claimedAt " +
			"WHERE o.id = :id " +
			"AND (o.status = org.pgsg.common.domain.OutboxStatus.PENDING " +
			"OR o.status = org.pgsg.common.domain.OutboxStatus.FAILED)")
	int updateStatusIfReady(@Param("id") UUID id,
							@Param("outboxStatus") OutboxStatus outboxStatus,
							@Param("claimedAt") LocalDateTime claimedAt);

	List<Outbox> findAllByCorrelationId(UUID uuid);

	@Modifying
	@Query("UPDATE Outbox o SET o.status = org.pgsg.common.domain.OutboxStatus.PENDING " +
			"WHERE o.status = org.pgsg.common.domain.OutboxStatus.SENDING " +
			"AND o.claimedAt < :threshold")
	int reclaimStuckSending(@Param("threshold") LocalDateTime threshold);

	@Query("SELECT o FROM Outbox o " +
			"WHERE o.status IN :statuses AND o.retryCount < :maxRetry " +
			"ORDER BY o.createdAt ASC")
	List<Outbox> findRetryableMessages(@Param("statuses") List<OutboxStatus> statuses,
									   @Param("maxRetry") int maxRetry,
									   Pageable pageable);
}
