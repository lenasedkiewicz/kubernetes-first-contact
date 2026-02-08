package com.lenasedkiewicz.frontend.controller;

import com.lenasedkiewicz.frontend.dto.TaskDto;
import com.lenasedkiewicz.frontend.dto.TaskFormDto;
import com.lenasedkiewicz.frontend.enums.Priority;
import com.lenasedkiewicz.frontend.enums.Status;
import com.lenasedkiewicz.frontend.service.TaskboardApiClient;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class KanbanController {

    private static final Logger logger = LoggerFactory.getLogger(KanbanController.class);

    private final TaskboardApiClient apiClient;

    public KanbanController(TaskboardApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @GetMapping("/")
    public String kanbanBoard(Model model) {
        List<TaskDto> allTasks = apiClient.getAllTasks();

        Map<Status, List<TaskDto>> tasksByStatus = allTasks.stream()
                .collect(Collectors.groupingBy(TaskDto::getStatus));

        model.addAttribute("todoTasks", tasksByStatus.getOrDefault(Status.TO_DO, List.of()));
        model.addAttribute("inProgressTasks", tasksByStatus.getOrDefault(Status.IN_PROGRESS, List.of()));
        model.addAttribute("doneTasks", tasksByStatus.getOrDefault(Status.DONE, List.of()));
        model.addAttribute("taskForm", new TaskFormDto());
        model.addAttribute("priorities", Priority.values());
        model.addAttribute("statuses", Status.values());

        return "kanban";
    }

    @PostMapping("/tasks")
    public String createTask(@Valid @ModelAttribute("taskForm") TaskFormDto form,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the form errors");
            return "redirect:/";
        }

        try {
            apiClient.createTask(form);
            redirectAttributes.addFlashAttribute("success", "Task created successfully (processing async - refresh if not visible)");
        } catch (Exception e) {
            logger.error("Failed to create task: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create task: " + e.getMessage());
        }

        return "redirect:/";
    }

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
            TaskFormDto form = new TaskFormDto();
            form.setId(task.getId());
            form.setName(task.getName());
            form.setDurationMinutes(task.getDurationMinutes());
            form.setPriority(task.getPriority());
            form.setStatus(task.getStatus());

            model.addAttribute("taskForm", form);
            model.addAttribute("priorities", Priority.values());
            model.addAttribute("statuses", Status.values());
            model.addAttribute("isEdit", true);

            // Also load tasks for the board display
            List<TaskDto> allTasks = apiClient.getAllTasks();
            Map<Status, List<TaskDto>> tasksByStatus = allTasks.stream()
                    .collect(Collectors.groupingBy(TaskDto::getStatus));
            model.addAttribute("todoTasks", tasksByStatus.getOrDefault(Status.TO_DO, List.of()));
            model.addAttribute("inProgressTasks", tasksByStatus.getOrDefault(Status.IN_PROGRESS, List.of()));
            model.addAttribute("doneTasks", tasksByStatus.getOrDefault(Status.DONE, List.of()));

            return "kanban";

        } catch (TaskboardApiClient.TaskboardApiException e) {
            logger.error("API error while fetching task {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Error communicating with API: " + e.getMessage() +
                    ". Check if all services (taskboard-api, tasks-service, kafka) are running.");
            return "redirect:/";
        } catch (Exception e) {
            logger.error("Unexpected error while fetching task {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error",
                    "Unexpected error: " + e.getMessage() +
                    ". Check the logs for more details.");
            return "redirect:/";
        }
    }

    @PostMapping("/tasks/{id}")
    public String updateTask(@PathVariable Long id,
                             @Valid @ModelAttribute("taskForm") TaskFormDto form,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Please fix the form errors");
            return "redirect:/";
        }

        try {
            apiClient.updateTask(id, form);
            redirectAttributes.addFlashAttribute("success", "Task update submitted (processing async)");
        } catch (Exception e) {
            logger.error("Failed to update task {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update task: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/move-left")
    public String moveTaskLeft(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<TaskDto> taskOpt = apiClient.getTaskById(id);
            if (taskOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Task not found (id=" + id + ")");
                return "redirect:/";
            }

            Status newStatus = getPreviousStatus(taskOpt.get().getStatus());
            if (newStatus != null) {
                apiClient.updateTaskStatus(id, newStatus);
                redirectAttributes.addFlashAttribute("success", "Task move submitted (processing async)");
            }
        } catch (Exception e) {
            logger.error("Failed to move task {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to move task: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/move-right")
    public String moveTaskRight(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<TaskDto> taskOpt = apiClient.getTaskById(id);
            if (taskOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Task not found (id=" + id + ")");
                return "redirect:/";
            }

            Status newStatus = getNextStatus(taskOpt.get().getStatus());
            if (newStatus != null) {
                apiClient.updateTaskStatus(id, newStatus);
                redirectAttributes.addFlashAttribute("success", "Task move submitted (processing async)");
            }
        } catch (Exception e) {
            logger.error("Failed to move task {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to move task: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            apiClient.deleteTask(id);
            redirectAttributes.addFlashAttribute("success", "Task delete submitted (processing async)");
        } catch (Exception e) {
            logger.error("Failed to delete task {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete task: " + e.getMessage());
        }

        return "redirect:/";
    }

    private Status getPreviousStatus(Status current) {
        return switch (current) {
            case IN_PROGRESS -> Status.TO_DO;
            case DONE -> Status.IN_PROGRESS;
            default -> null;
        };
    }

    private Status getNextStatus(Status current) {
        return switch (current) {
            case TO_DO -> Status.IN_PROGRESS;
            case IN_PROGRESS -> Status.DONE;
            default -> null;
        };
    }
}
