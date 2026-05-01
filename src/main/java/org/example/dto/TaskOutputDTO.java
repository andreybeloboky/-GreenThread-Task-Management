package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.example.controller.TaskStatus;

import java.time.Instant;

@Getter
@Setter
public class TaskOutputDTO {

    @JsonProperty
    private int id;

    @JsonProperty
    private String title;

    @JsonProperty
    private String description;

    @JsonProperty
    private Instant date;

    @JsonProperty
    private TaskStatus status;
}
