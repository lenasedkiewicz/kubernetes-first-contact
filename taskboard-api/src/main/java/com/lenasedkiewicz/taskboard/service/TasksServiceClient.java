package com.lenasedkiewicz.taskboard.service;

import com.lenasedkiewicz.taskboard.dto.TaskResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;

@Service
public class TasksServiceClient {

    private final WebClient webClient;

    public TasksServiceClient(@Value("${tasks.service.base-url:http://localhost:8082}") String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<TaskResponse> getAllTasks() {
        return webClient.get()
                .uri("/api/tasks")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<TaskResponse>>() {})
                .block();
    }

    public Optional<TaskResponse> getTaskById(Long id) {
        try {
            TaskResponse response = webClient.get()
                    .uri("/api/tasks/{id}", id)
                    .retrieve()
                    .bodyToMono(TaskResponse.class)
                    .block();
            return Optional.ofNullable(response);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
