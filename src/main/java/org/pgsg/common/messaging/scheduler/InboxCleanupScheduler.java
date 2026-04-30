package org.pgsg.common.messaging.scheduler;

import java.time.LocalDateTime;

import org.pgsg.common.messaging.processor.InboxCleanupProcessor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class InboxCleanupScheduler {

	private final InboxCleanupProcessor inboxCleanupProcessor;

	private static final int CHUNK_SIZE = 1000;
	private static final int RETENTION_DAYS = 7;

	@Scheduled(cron = "0 0 3 * * *")
	@SchedulerLock(name = "inbox-cleanup-lock", lockAtMostFor = "PT1H", lockAtLeastFor = "PT5M")
	public void cleanupOldMessage() {
		LocalDateTime threshold = LocalDateTime.now().minusDays(RETENTION_DAYS);
		int totalDeleted = 0;

		log.info("Inbox 정리 스케줄러 실행: {} 이전 데이터 삭제 시작", threshold);

		while (true) {
			int deletedCount = inboxCleanupProcessor.deleteChunk(threshold, CHUNK_SIZE);

			if (deletedCount == 0) {
				break;
			}

			totalDeleted += deletedCount;

			if (deletedCount < CHUNK_SIZE) {
				break;
			}
		}

		if (totalDeleted > 0) {
			log.info("처리 완료된 7일 경과 Inbox 내역 삭제: {}건 삭제됨", totalDeleted);
		}
	}
}
