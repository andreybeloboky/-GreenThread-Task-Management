package org.example.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class Subtask {
    private int id;
    private int taskId;
    private String title;
    private boolean completed;
    private Instant created_at;
}
