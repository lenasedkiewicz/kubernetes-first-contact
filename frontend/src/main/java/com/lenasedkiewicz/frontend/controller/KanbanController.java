package com.lenasedkiewicz.frontend.controller;

import com.lenasedkiewicz.frontend.dto.TaskDto;
import com.lenasedkiewicz.frontend.dto.TaskFormDto;
import com.lenasedkiewicz.frontend.enums.Priority;
import com.lenasedkiewicz.frontend.enums.Status;
import com.lenasedkiewicz.frontend.service.TaskboardApiClient;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class KanbanController {

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
            redirectAttributes.addFlashAttribute("success", "Task created successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create task: " + e.getMessage());
        }

        return "redirect:/";
    }

    @GetMapping("/tasks/{id}/edit")
    public String editTaskForm(@PathVariable Long id, Model model) {
        TaskDto task = apiClient.getTaskById(id);

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

        return "kanban";
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
            redirectAttributes.addFlashAttribute("success", "Task updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update task: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/move-left")
    public String moveTaskLeft(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TaskDto task = apiClient.getTaskById(id);
            Status newStatus = getPreviousStatus(task.getStatus());
            if (newStatus != null) {
                apiClient.updateTaskStatus(id, newStatus);
                redirectAttributes.addFlashAttribute("success", "Task moved successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to move task: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/move-right")
    public String moveTaskRight(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            TaskDto task = apiClient.getTaskById(id);
            Status newStatus = getNextStatus(task.getStatus());
            if (newStatus != null) {
                apiClient.updateTaskStatus(id, newStatus);
                redirectAttributes.addFlashAttribute("success", "Task moved successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to move task: " + e.getMessage());
        }

        return "redirect:/";
    }

    @PostMapping("/tasks/{id}/delete")
    public String deleteTask(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            apiClient.deleteTask(id);
            redirectAttributes.addFlashAttribute("success", "Task deleted successfully");
        } catch (Exception e) {
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
