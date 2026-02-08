package com.lenasedkiewicz.tasks.event;

import com.lenasedkiewicz.tasks.enums.Priority;
import com.lenasedkiewicz.tasks.enums.Status;

import java.time.LocalDateTime;

public class TaskEvent {

    private String eventType; // CREATE, UPDATE, STATUS_UPDATE, DELETE
    private String eventId;   // UUID for idempotency
    private Long taskId;      // null for CREATE
    private TaskPayload payload;
    private LocalDateTime timestamp;

    public TaskEvent() {
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public TaskPayload getPayload() {
        return payload;
    }

    public void setPayload(TaskPayload payload) {
        this.payload = payload;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public static class TaskPayload {
        private String name;
        private Integer durationMinutes;
        private Priority priority;
        private Status status;

        public TaskPayload() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getDurationMinutes() {
            return durationMinutes;
        }

        public void setDurationMinutes(Integer durationMinutes) {
            this.durationMinutes = durationMinutes;
        }

        public Priority getPriority() {
            return priority;
        }

        public void setPriority(Priority priority) {
            this.priority = priority;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }
    }
}
