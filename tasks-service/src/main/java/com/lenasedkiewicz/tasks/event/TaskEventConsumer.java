package com.lenasedkiewicz.tasks.event;

import com.lenasedkiewicz.tasks.service.TaskPersistenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class TaskEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TaskEventConsumer.class);

    private final TaskPersistenceService taskPersistenceService;

    public TaskEventConsumer(TaskPersistenceService taskPersistenceService) {
        this.taskPersistenceService = taskPersistenceService;
    }

    @KafkaListener(topics = "${kafka.topic.task-events:task-events}", groupId = "${spring.kafka.consumer.group-id:tasks-service}")
    public void consume(TaskEvent event) {
        logger.info("Received event: {} for taskId: {}", event.getEventType(), event.getTaskId());
        try {
            taskPersistenceService.processEvent(event);
        } catch (Exception e) {
            logger.error("Error processing event: {}", event.getEventId(), e);
        }
    }
}
