package com.lenasedkiewicz.taskboard.service;

import com.lenasedkiewicz.taskboard.dto.StatusUpdateRequest;
import com.lenasedkiewicz.taskboard.dto.TaskCreateRequest;
import com.lenasedkiewicz.taskboard.dto.TaskResponse;
import com.lenasedkiewicz.taskboard.dto.TaskUpdateRequest;
import com.lenasedkiewicz.taskboard.event.TaskEvent;
import com.lenasedkiewicz.taskboard.event.TaskEventProducer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TaskService {

    private final TaskEventProducer eventProducer;
    private final TasksServiceClient tasksServiceClient;

    public TaskService(TaskEventProducer eventProducer, TasksServiceClient tasksServiceClient) {
        this.eventProducer = eventProducer;
        this.tasksServiceClient = tasksServiceClient;
    }

    public List<TaskResponse> getAllTasks() {
        return tasksServiceClient.getAllTasks();
    }

    public Optional<TaskResponse> getTaskById(Long id) {
        return tasksServiceClient.getTaskById(id);
    }

    public void createTask(TaskCreateRequest request) {
        TaskEvent event = TaskEvent.createEvent(
                request.getName(),
                request.getDurationMinutes(),
                request.getPriority()
        );
        eventProducer.send(event);
    }

    public void updateTask(Long id, TaskUpdateRequest request) {
        TaskEvent event = TaskEvent.updateEvent(
                id,
                request.getName(),
                request.getDurationMinutes(),
                request.getPriority(),
                request.getStatus()
        );
        eventProducer.send(event);
    }

    public void updateTaskStatus(Long id, StatusUpdateRequest request) {
        TaskEvent event = TaskEvent.statusUpdateEvent(id, request.getStatus());
        eventProducer.send(event);
    }

    public void deleteTask(Long id) {
        TaskEvent event = TaskEvent.deleteEvent(id);
        eventProducer.send(event);
    }
}
