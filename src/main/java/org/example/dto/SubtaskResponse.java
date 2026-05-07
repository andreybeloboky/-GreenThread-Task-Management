package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.example.model.Subtask;

@Getter
public class SubtaskResponse {

    @JsonProperty
    private int id;

    @JsonProperty
    private int taskId;

    @JsonProperty
    private String title;

    @JsonProperty
    private boolean completed;

    public SubtaskResponse(Subtask subtask) {
        this.id = subtask.getId();
        this.taskId = subtask.getTask_id();
        this.title = subtask.getTitle();
        this.completed = subtask.isCompleted();
    }
}
