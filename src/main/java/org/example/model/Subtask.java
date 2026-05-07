package org.example.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Subtask {
    private int id;
    private int task_id;
    private String title;
    private boolean completed;
}
