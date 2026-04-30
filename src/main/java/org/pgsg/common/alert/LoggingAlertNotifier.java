package org.pgsg.common.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
public class LoggingAlertNotifier implements AlertNotifier {

    @Override
    public void notifyDltFailure(UUID messageId, String eventType, Throwable cause) {
        log.error("[ALERT][DLT_FAILURE] messageId={}, eventType={}", messageId, eventType, cause);
    }

}
