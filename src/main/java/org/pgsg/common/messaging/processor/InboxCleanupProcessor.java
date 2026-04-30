package org.pgsg.common.messaging.processor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.pgsg.common.domain.InboxStatus;
import org.pgsg.common.domain.InboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InboxCleanupProcessor {

    private final InboxRepository inboxRepository;

    @Transactional
    public int deleteChunk(LocalDateTime threshold, int chunkSize) {
        List<UUID> targetIds = inboxRepository.findIdsForCleanup(
                InboxStatus.PROCESSED,
                threshold,
                PageRequest.of(0, chunkSize)
        );

        if (targetIds.isEmpty()) {
            return 0;
        }

        int deletedCount = inboxRepository.deleteByIds(targetIds);
        inboxRepository.flush();

        return deletedCount;
    }
}
