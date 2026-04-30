package org.pgsg.common.domain;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID>, QuerydslPredicateExecutor<Outbox> {

	@Modifying
	@Query("UPDATE Outbox o SET o.status = :outboxStatus " +
		"WHERE o.id = :id " +
		"AND (o.status = org.pgsg.common.domain.OutboxStatus.PENDING " +
		"OR o.status = org.pgsg.common.domain.OutboxStatus.FAILED)")
	int updateStatusIfReady(@Param("id") UUID id, @Param("outboxStatus") OutboxStatus outboxStatus);

	List<Outbox> findAllByCorrelationId(UUID uuid);

    List<Outbox> findAll(BooleanExpression and, OrderSpecifier<LocalDateTime> asc, int fetchSize);
}
