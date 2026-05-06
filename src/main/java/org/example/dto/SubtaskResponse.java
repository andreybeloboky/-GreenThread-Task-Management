package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.example.model.Subtask;

public class SubtaskResponse {
    @JsonProperty
    private int taskId;

    @JsonProperty
    private String title;

    @JsonProperty
    private boolean completed;

    public SubtaskResponse(Subtask subtask) {
        this.taskId = subtask.getTask_id();
        this.title = subtask.getTitle();
        this.completed = subtask.isCompleted();
    }
}
