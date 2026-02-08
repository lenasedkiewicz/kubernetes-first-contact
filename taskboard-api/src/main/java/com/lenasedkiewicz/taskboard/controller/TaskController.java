package com.lenasedkiewicz.taskboard.controller;

import com.lenasedkiewicz.taskboard.dto.StatusUpdateRequest;
import com.lenasedkiewicz.taskboard.dto.TaskCreateRequest;
import com.lenasedkiewicz.taskboard.dto.TaskResponse;
import com.lenasedkiewicz.taskboard.dto.TaskUpdateRequest;
import com.lenasedkiewicz.taskboard.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return taskService.getAllTasks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return taskService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Void> createTask(@Valid @RequestBody TaskCreateRequest request) {
        taskService.createTask(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateTask(@PathVariable Long id, @Valid @RequestBody TaskUpdateRequest request) {
        taskService.updateTask(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateTaskStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        taskService.updateTaskStatus(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
