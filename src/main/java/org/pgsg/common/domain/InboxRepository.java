package org.pgsg.common.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;

public interface InboxRepository extends JpaRepository<Inbox, UUID>, QuerydslPredicateExecutor<Inbox> {

    @Query("SELECT i.id FROM Inbox i WHERE i.status = :status AND i.processedAt < :threshold")
    List<UUID> findIdsForCleanup(@Param("status") InboxStatus status,
                                 @Param("threshold") LocalDateTime threshold,
                                 Pageable pageable);

    @Modifying
    @Query("DELETE FROM Inbox i WHERE i.id IN :ids")
    int deleteByIds(@Param("ids") List<UUID> ids);
}
