package org.pgsg.common.event;

import lombok.NoArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.pgsg.common.domain.Outbox;

@NoArgsConstructor
public final class OutboxMessageFactory {

    public static final String HEADER_MESSAGE_ID = "message_id";
    public static final String HEADER_CORRELATION_ID = "correlation_id";
    public static final String HEADER_ERROR_REASON = "error_reason";

    public static ProducerRecord<String, Object> toRecord(Outbox outbox) {
        return toRecord(outbox, outbox.getEventType());
    }

    public static ProducerRecord<String, Object> toRecord(Outbox outbox, String topic) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                topic,
                outbox.getDomainId().toString(),
                outbox.getPayload()
        );

        record.headers().add(HEADER_MESSAGE_ID, outbox.getId().toString().getBytes());
        record.headers().add(HEADER_CORRELATION_ID, outbox.getCorrelationId().toString().getBytes());

        return record;
    }

    public static ProducerRecord<String, Object> toDltRecord(Outbox outbox, String reason) {
        ProducerRecord<String, Object> record = toRecord(outbox, outbox.getDltTopic());
        record.headers().add(HEADER_ERROR_REASON, reason.getBytes());

        return record;
    }
}
