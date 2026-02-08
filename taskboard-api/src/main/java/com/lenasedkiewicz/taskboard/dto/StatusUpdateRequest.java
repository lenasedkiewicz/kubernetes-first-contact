package com.lenasedkiewicz.taskboard.dto;

import com.lenasedkiewicz.taskboard.enums.Status;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateRequest {

    @NotNull(message = "Status is required")
    private Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
