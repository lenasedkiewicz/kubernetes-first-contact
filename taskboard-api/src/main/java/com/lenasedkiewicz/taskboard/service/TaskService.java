package com.lenasedkiewicz.taskboard.service;

import com.lenasedkiewicz.taskboard.dto.StatusUpdateRequest;
import com.lenasedkiewicz.taskboard.dto.TaskCreateRequest;
import com.lenasedkiewicz.taskboard.dto.TaskResponse;
import com.lenasedkiewicz.taskboard.dto.TaskUpdateRequest;
import com.lenasedkiewicz.taskboard.entity.Task;
import com.lenasedkiewicz.taskboard.enums.Status;
import com.lenasedkiewicz.taskboard.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TaskService {

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    public List<TaskResponse> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(TaskResponse::new)
                .toList();
    }

    public TaskResponse getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));
        return new TaskResponse(task);
    }

    public TaskResponse createTask(TaskCreateRequest request) {
        Task task = new Task();
        task.setName(request.getName());
        task.setDurationMinutes(request.getDurationMinutes());
        task.setPriority(request.getPriority());
        task.setStatus(Status.TO_DO);

        Task savedTask = taskRepository.save(task);
        return new TaskResponse(savedTask);
    }

    public TaskResponse updateTask(Long id, TaskUpdateRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));

        task.setName(request.getName());
        task.setDurationMinutes(request.getDurationMinutes());
        task.setPriority(request.getPriority());
        task.setStatus(request.getStatus());

        Task updatedTask = taskRepository.save(task);
        return new TaskResponse(updatedTask);
    }

    public TaskResponse updateTaskStatus(Long id, StatusUpdateRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with id: " + id));

        task.setStatus(request.getStatus());

        Task updatedTask = taskRepository.save(task);
        return new TaskResponse(updatedTask);
    }

    public void deleteTask(Long id) {
        if (!taskRepository.existsById(id)) {
            throw new TaskNotFoundException("Task not found with id: " + id);
        }
        taskRepository.deleteById(id);
    }

    public static class TaskNotFoundException extends RuntimeException {
        public TaskNotFoundException(String message) {
            super(message);
        }
    }
}
