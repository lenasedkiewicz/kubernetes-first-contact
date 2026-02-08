package com.lenasedkiewicz.taskboard.event;

import com.lenasedkiewicz.taskboard.enums.Priority;
import com.lenasedkiewicz.taskboard.enums.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public class TaskEvent {

    private String eventType; // CREATE, UPDATE, STATUS_UPDATE, DELETE
    private String eventId;   // UUID for idempotency
    private Long taskId;      // null for CREATE
    private TaskPayload payload;
    private LocalDateTime timestamp;

    public TaskEvent() {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }

    public static TaskEvent createEvent(String name, Integer durationMinutes, Priority priority) {
        TaskEvent event = new TaskEvent();
        event.setEventType("CREATE");
        TaskPayload payload = new TaskPayload();
        payload.setName(name);
        payload.setDurationMinutes(durationMinutes);
        payload.setPriority(priority);
        payload.setStatus(Status.TO_DO);
        event.setPayload(payload);
        return event;
    }

    public static TaskEvent updateEvent(Long taskId, String name, Integer durationMinutes, Priority priority, Status status) {
        TaskEvent event = new TaskEvent();
        event.setEventType("UPDATE");
        event.setTaskId(taskId);
        TaskPayload payload = new TaskPayload();
        payload.setName(name);
        payload.setDurationMinutes(durationMinutes);
        payload.setPriority(priority);
        payload.setStatus(status);
        event.setPayload(payload);
        return event;
    }

    public static TaskEvent statusUpdateEvent(Long taskId, Status status) {
        TaskEvent event = new TaskEvent();
        event.setEventType("STATUS_UPDATE");
        event.setTaskId(taskId);
        TaskPayload payload = new TaskPayload();
        payload.setStatus(status);
        event.setPayload(payload);
        return event;
    }

    public static TaskEvent deleteEvent(Long taskId) {
        TaskEvent event = new TaskEvent();
        event.setEventType("DELETE");
        event.setTaskId(taskId);
        return event;
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
