package org.example.controller;

import java.util.Set;

public enum TaskStatus {

    COMPLETED(Set.of()),
    IN_PROGRESS(Set.of(COMPLETED)),
    PENDING(Set.of(IN_PROGRESS));

    private final Set<TaskStatus> allowed;

    TaskStatus(Set<TaskStatus> allowed) {
        this.allowed = allowed;
    }

    public boolean canTransitionTo(TaskStatus next) {
        return allowed.contains(next);
    }
}
