package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.DTO.TaskDTO;
import org.example.controller.TaskStatus;
import org.example.repository.JDBCRepository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public class TaskService {

    private final JDBCRepository repo;
    private final Validator validator;

    public TaskService(HikariDataSource ds, Validator validatorIn) {
        this.repo = new JDBCRepository(ds);
        this.validator = validatorIn;
    }

    public ArrayList<TaskDTO> takeAllElements() {
        return repo.getList();
    }

    public TaskDTO createTask(TaskDTO createTask) {
        Set<ConstraintViolation<TaskDTO>> violations = validator.validate(createTask);
        if (!violations.isEmpty()) {
            return null;
        }
        repo.insert(createTask);
        return createTask;
    }

    public Optional<TaskDTO> update(int id, Optional<TaskDTO> task) {
        TaskDTO oldTask = repo.findById(id);
        if (oldTask == null) return Optional.empty();

        TaskStatus oldStatus = oldTask.getStatus();
        TaskStatus newStatus = task.get().getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition");
        }
        this.repo.setUpdate(task.get(), id);
        return task;
    }

    public boolean delete(int id) {
        return this.repo.delete(id);
    }
}
