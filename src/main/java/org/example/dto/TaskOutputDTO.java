package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.controller.TaskStatus;

import java.time.LocalDateTime;

@Getter
@Setter
public class TaskOutputDTO {

    @JsonProperty
    private String title;

    @JsonProperty
    private String description;

    @JsonProperty
    private LocalDateTime date;

    @JsonProperty
    private TaskStatus status;
}
