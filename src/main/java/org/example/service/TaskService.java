package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.DTO.TaskDTO;
import org.example.controller.TaskStatus;
import org.example.repository.TaskJDBCRepository;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;

public class TaskService {

    private final TaskJDBCRepository repo;
    private final Validator validator;

    public TaskService(HikariDataSource ds, Validator validatorIn) {
        this.repo = new TaskJDBCRepository(ds);
        this.validator = validatorIn;
    }

    public ArrayList<TaskDTO> takeAllElements() {
        return repo.getList();
    }

    public Optional<TaskDTO> createTask(TaskDTO createTask) {
        Optional<TaskDTO> theSameTitleName = repo.findByTitle(createTask.getTitle());
        if (theSameTitleName.isPresent()) {
            return Optional.empty();
        }

        Set<ConstraintViolation<TaskDTO>> violations = validator.validate(createTask);
        if (!violations.isEmpty()) return Optional.empty();

        repo.insert(createTask);
        return Optional.of(createTask);
    }

    public Optional<TaskDTO> update(int id, TaskDTO task) {
        Optional<TaskDTO> oldTask = repo.findById(id);
        if (oldTask.isEmpty()) return Optional.empty();

        TaskStatus oldStatus = oldTask.get().getStatus();
        TaskStatus newStatus = task.getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition");
        }
        this.repo.setUpdate(task, id);
        return Optional.of(task);
    }

    public boolean delete(int id) {
        return this.repo.delete(id);
    }
}
