package com.lenasedkiewicz.tasks.controller;

import com.lenasedkiewicz.tasks.dto.TaskResponse;
import com.lenasedkiewicz.tasks.service.TaskPersistenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskReadController {

    private final TaskPersistenceService taskPersistenceService;

    public TaskReadController(TaskPersistenceService taskPersistenceService) {
        this.taskPersistenceService = taskPersistenceService;
    }

    @GetMapping
    public List<TaskResponse> getAllTasks() {
        return taskPersistenceService.getAllTasks();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return taskPersistenceService.getTaskById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
