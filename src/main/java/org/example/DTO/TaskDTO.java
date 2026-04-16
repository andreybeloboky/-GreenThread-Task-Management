package org.example.DTO;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskDTO {
    @NotBlank(message = "Title cannot be empty")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;
    @NotNull(message = "Due date is required")
    @Future(message = "Due date must be in the future")
    private LocalDateTime date;
    @NotNull
    private String status;
}
