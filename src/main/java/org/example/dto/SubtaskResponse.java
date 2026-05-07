package org.example.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.example.model.Subtask;

import java.time.Instant;
import java.time.OffsetDateTime;

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

    @JsonProperty
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Instant created_at;

    public SubtaskResponse(Subtask subtask) {
        this.id = subtask.getId();
        this.taskId = subtask.getTask_id();
        this.title = subtask.getTitle();
        this.completed = subtask.isCompleted();
        this.created_at = subtask.getCreated_at();
    }
}
