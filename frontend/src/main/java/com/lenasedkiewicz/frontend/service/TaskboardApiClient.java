package com.lenasedkiewicz.frontend.service;

import com.lenasedkiewicz.frontend.dto.TaskDto;
import com.lenasedkiewicz.frontend.dto.TaskFormDto;
import com.lenasedkiewicz.frontend.enums.Status;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class TaskboardApiClient {

    private final WebClient webClient;

    public TaskboardApiClient(@Value("${taskboard.api.base-url}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<TaskDto> getAllTasks() {
        return webClient.get()
                .uri("/api/tasks")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TaskDto>>() {})
                .block();
    }

    public TaskDto getTaskById(Long id) {
        return webClient.get()
                .uri("/api/tasks/{id}", id)
                .retrieve()
                .bodyToMono(TaskDto.class)
                .block();
    }

    public TaskDto createTask(TaskFormDto form) {
        return webClient.post()
                .uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "name", form.getName(),
                        "durationMinutes", form.getDurationMinutes(),
                        "priority", form.getPriority()
                ))
                .retrieve()
                .bodyToMono(TaskDto.class)
                .block();
    }

    public TaskDto updateTask(Long id, TaskFormDto form) {
        return webClient.put()
                .uri("/api/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "name", form.getName(),
                        "durationMinutes", form.getDurationMinutes(),
                        "priority", form.getPriority(),
                        "status", form.getStatus()
                ))
                .retrieve()
                .bodyToMono(TaskDto.class)
                .block();
    }

    public TaskDto updateTaskStatus(Long id, Status status) {
        return webClient.patch()
                .uri("/api/tasks/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", status))
                .retrieve()
                .bodyToMono(TaskDto.class)
                .block();
    }

    public void deleteTask(Long id) {
        webClient.delete()
                .uri("/api/tasks/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .block();
    }
}
