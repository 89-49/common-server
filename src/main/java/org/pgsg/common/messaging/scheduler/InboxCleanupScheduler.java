package org.pgsg.common.messaging.scheduler;

import java.time.LocalDateTime;

import org.pgsg.common.domain.InboxStatus;
import org.pgsg.common.domain.QInbox;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class InboxCleanupScheduler {
	private final JPAQueryFactory queryFactory;

	@Transactional
	@Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 정리 작업 진행
	public void cleanupOldMessage() {
		// 7일 이전 목록 삭제
		long count = queryFactory
			.delete(QInbox.inbox)
			.where(
				QInbox.inbox.status.eq(InboxStatus.PROCESSED),
				QInbox.inbox.receivedAt.before(LocalDateTime.now().minusWeeks(1L)	//todo: 시간 측정 기준을 위해 처리일도 추가할지 검토
				))
			.execute();

		log.info("처리 완료된 7일 경과 Inbox 내역 삭제: {}건 삭제됨", count);
	}
}
