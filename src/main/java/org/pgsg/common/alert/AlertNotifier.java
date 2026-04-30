package org.pgsg.common.alert;

import java.util.UUID;

public interface AlertNotifier {

    void notifyDltFailure(UUID messageId, String eventType, Throwable cause);

}
