package org.pgsg.common.domain;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

public interface InboxRepository extends JpaRepository<Inbox, UUID>, QuerydslPredicateExecutor<Inbox> {
}
