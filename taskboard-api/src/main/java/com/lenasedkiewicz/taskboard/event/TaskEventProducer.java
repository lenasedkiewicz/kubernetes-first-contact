package com.lenasedkiewicz.taskboard.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class TaskEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(TaskEventProducer.class);

    private final KafkaTemplate<String, TaskEvent> kafkaTemplate;
    private final String topicName;

    public TaskEventProducer(
            KafkaTemplate<String, TaskEvent> kafkaTemplate,
            @Value("${kafka.topic.task-events:task-events}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void send(TaskEvent event) {
        logger.info("Sending event: {} with eventId: {}", event.getEventType(), event.getEventId());
        String key = event.getTaskId() != null ? event.getTaskId().toString() : event.getEventId();
        kafkaTemplate.send(topicName, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        logger.error("Failed to send event: {}", event.getEventId(), ex);
                    } else {
                        logger.info("Event sent successfully: {} to partition: {}",
                                event.getEventId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}
