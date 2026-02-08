# Fix: WebClient 404 Unhandled Exception Causing Whitelabel Error

## Error

```
Whitelabel Error Page
This application has no explicit mapping for /error, so you are seeing this as a fallback.

There was an unexpected error (type=Internal Server Error, status=500)
```

Occurs when trying to edit a task that doesn't exist or hasn't been processed yet.

## Root Cause

The frontend's `TaskboardApiClient.getTaskById()` was returning `TaskDto` directly:

```java
public TaskDto getTaskById(Long id) {
    return webClient.get()
            .uri("/api/tasks/{id}", id)
            .retrieve()
            .bodyToMono(TaskDto.class)
            .block();
}
```

**Problems:**

1. **WebClient throws exception on 404**: When the API returns 404, WebClient throws `WebClientResponseException.NotFound` instead of returning null
2. **No exception handling in controller**: The `editTaskForm` method didn't have try-catch:
   ```java
   @GetMapping("/tasks/{id}/edit")
   public String editTaskForm(@PathVariable Long id, Model model) {
       TaskDto task = apiClient.getTaskById(id);  // Throws exception on 404!
       // NullPointerException if task is null
       form.setName(task.getName());
   }
   ```
3. **Async architecture compounds the issue**: With Kafka-based async processing, a task may not exist in the database immediately after creation, making 404 errors more common

## Solution

### 1. Return Optional from API client

```java
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
    }
}
```

### 2. Handle Optional in controller with informative messages

```java
@GetMapping("/tasks/{id}/edit")
public String editTaskForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
    try {
        Optional<TaskDto> taskOpt = apiClient.getTaskById(id);

        if (taskOpt.isEmpty()) {
            logger.warn("Task with id {} not found - may not have been processed yet (async)", id);
            redirectAttributes.addFlashAttribute("error",
                    "Task not found (id=" + id + "). " +
                    "This may happen if the task was just created and Kafka hasn't processed it yet. " +
                    "Please wait a moment and refresh the page.");
            return "redirect:/";
        }

        TaskDto task = taskOpt.get();
        // ... continue with task

    } catch (TaskboardApiException e) {
        logger.error("API error: {}", e.getMessage(), e);
        redirectAttributes.addFlashAttribute("error",
                "Error communicating with API: " + e.getMessage() +
                ". Check if all services are running.");
        return "redirect:/";
    }
}
```

### 3. Create custom exception for API errors

```java
public static class TaskboardApiException extends RuntimeException {
    public TaskboardApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## Informative Error Messages

The fix provides context-aware error messages:

| Scenario | Error Message |
|----------|---------------|
| Task not found (404) | "Task not found (id=X). This may happen if the task was just created and Kafka hasn't processed it yet. Please wait a moment and refresh the page." |
| API communication error | "Error communicating with API: [details]. Check if all services (taskboard-api, tasks-service, kafka) are running." |
| Unexpected error | "Unexpected error: [details]. Check the logs for more details." |

## Key Takeaways

1. **WebClient throws exceptions on HTTP errors** - Always handle `WebClientResponseException` variants
2. **Use Optional for nullable results** - Makes null-handling explicit and compiler-enforced
3. **Async systems need graceful degradation** - In event-driven architectures, data may not be immediately available
4. **Provide actionable error messages** - Tell users what happened AND what to do about it
5. **Log with context** - Include IDs, status codes, and response bodies for debugging
