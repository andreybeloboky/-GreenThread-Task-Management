package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import org.example.controller.TaskStatus;
import org.example.model.Task;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TaskResponse {

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

    @JsonProperty
    private List<SubtaskResponse> subtasks;

    public TaskResponse(Task task) {
        this.id = task.getId();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.date = task.getDate();
        this.status = task.getStatus();
        this.subtasks = task.getSubtasks()
                .stream()
                .map(SubtaskResponse::new)
                .collect(Collectors.toList());
    }
}
