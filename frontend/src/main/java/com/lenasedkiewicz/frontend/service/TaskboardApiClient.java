package com.lenasedkiewicz.frontend.service;

import com.lenasedkiewicz.frontend.dto.TaskDto;
import com.lenasedkiewicz.frontend.dto.TaskFormDto;
import com.lenasedkiewicz.frontend.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TaskboardApiClient {

    private static final Logger logger = LoggerFactory.getLogger(TaskboardApiClient.class);

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

    public Optional<TaskDto> getTaskById(Long id) {
        try {
            TaskDto task = webClient.get()
                    .uri("/api/tasks/{id}", id)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, response -> {
                        logger.warn("Task with id {} not found, status: {}", id, response.statusCode());
                        return response.createException();
                    })
                    .bodyToMono(TaskDto.class)
                    .block();
            return Optional.ofNullable(task);
        } catch (WebClientResponseException.NotFound e) {
            logger.info("Task with id {} not found (404)", id);
            return Optional.empty();
        } catch (WebClientResponseException e) {
            logger.error("Error fetching task {}: {} - {}", id, e.getStatusCode(), e.getResponseBodyAsString());
            throw new TaskboardApiException("Failed to fetch task: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error fetching task {}: {}", id, e.getMessage());
            throw new TaskboardApiException("Failed to fetch task: " + e.getMessage(), e);
        }
    }

    public void createTask(TaskFormDto form) {
        webClient.post()
                .uri("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "name", form.getName(),
                        "durationMinutes", form.getDurationMinutes(),
                        "priority", form.getPriority()
                ))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void updateTask(Long id, TaskFormDto form) {
        webClient.put()
                .uri("/api/tasks/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "name", form.getName(),
                        "durationMinutes", form.getDurationMinutes(),
                        "priority", form.getPriority(),
                        "status", form.getStatus()
                ))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void updateTaskStatus(Long id, Status status) {
        webClient.patch()
                .uri("/api/tasks/{id}/status", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("status", status))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public void deleteTask(Long id) {
        webClient.delete()
                .uri("/api/tasks/{id}", id)
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    public static class TaskboardApiException extends RuntimeException {
        public TaskboardApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
