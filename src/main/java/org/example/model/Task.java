package org.example.model;

import lombok.Getter;
import lombok.Setter;
import org.example.controller.TaskStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class Task {
    private int id;
    private String title;
    private String description;
    private Instant date;
    private TaskStatus status;
    private int usernameId;
    List<Subtask> subtasks = new ArrayList<>();
}
