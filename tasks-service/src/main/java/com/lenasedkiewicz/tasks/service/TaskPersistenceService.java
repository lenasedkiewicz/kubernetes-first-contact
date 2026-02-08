package com.lenasedkiewicz.tasks.service;

import com.lenasedkiewicz.tasks.dto.TaskResponse;
import com.lenasedkiewicz.tasks.entity.Task;
import com.lenasedkiewicz.tasks.event.TaskEvent;
import com.lenasedkiewicz.tasks.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TaskPersistenceService {

    private static final Logger logger = LoggerFactory.getLogger(TaskPersistenceService.class);

    private final TaskRepository taskRepository;

    public TaskPersistenceService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(TaskResponse::new)
                .toList();
    }

    public Optional<TaskResponse> getTaskById(Long id) {
        return taskRepository.findById(id)
                .map(TaskResponse::new);
    }

    public void processEvent(TaskEvent event) {
        logger.info("Processing event: {} with eventId: {}", event.getEventType(), event.getEventId());

        switch (event.getEventType()) {
            case "CREATE" -> createTask(event);
            case "UPDATE" -> updateTask(event);
            case "STATUS_UPDATE" -> updateTaskStatus(event);
            case "DELETE" -> deleteTask(event);
            default -> logger.warn("Unknown event type: {}", event.getEventType());
        }
    }

    private void createTask(TaskEvent event) {
        TaskEvent.TaskPayload payload = event.getPayload();
        Task task = new Task();
        task.setName(payload.getName());
        task.setDurationMinutes(payload.getDurationMinutes());
        task.setPriority(payload.getPriority());
        task.setStatus(payload.getStatus() != null ? payload.getStatus() : com.lenasedkiewicz.tasks.enums.Status.TO_DO);

        Task saved = taskRepository.save(task);
        logger.info("Created task with id: {}", saved.getId());
    }

    private void updateTask(TaskEvent event) {
        Long taskId = event.getTaskId();
        TaskEvent.TaskPayload payload = event.getPayload();

        taskRepository.findById(taskId).ifPresentOrElse(
                task -> {
                    task.setName(payload.getName());
                    task.setDurationMinutes(payload.getDurationMinutes());
                    task.setPriority(payload.getPriority());
                    task.setStatus(payload.getStatus());
                    taskRepository.save(task);
                    logger.info("Updated task with id: {}", taskId);
                },
                () -> logger.warn("Task not found for UPDATE event: {}", taskId)
        );
    }

    private void updateTaskStatus(TaskEvent event) {
        Long taskId = event.getTaskId();
        TaskEvent.TaskPayload payload = event.getPayload();

        taskRepository.findById(taskId).ifPresentOrElse(
                task -> {
                    task.setStatus(payload.getStatus());
                    taskRepository.save(task);
                    logger.info("Updated status for task with id: {}", taskId);
                },
                () -> logger.warn("Task not found for STATUS_UPDATE event: {}", taskId)
        );
    }

    private void deleteTask(TaskEvent event) {
        Long taskId = event.getTaskId();
        if (taskRepository.existsById(taskId)) {
            taskRepository.deleteById(taskId);
            logger.info("Deleted task with id: {}", taskId);
        } else {
            logger.warn("Task not found for DELETE event: {}", taskId);
        }
    }
}
