package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.dto.TaskInputDTO;
import org.example.controller.TaskStatus;
import org.example.dto.TaskOutputDTO;
import org.example.exception.DataExistsException;
import org.example.repository.TaskJDBCRepository;

import java.util.ArrayList;
import java.util.Optional;


public class TaskService {

    private final TaskJDBCRepository repo;

    public TaskService(HikariDataSource ds) {
        this.repo = new TaskJDBCRepository(ds);
    }

    public ArrayList<TaskInputDTO> takeAllElements() {
        return repo.getList();
    }

    public TaskInputDTO createTask(TaskInputDTO createTask) {
        Optional<TaskInputDTO> theSameTitleName = repo.findByTitle(createTask.getTitle());

        if (theSameTitleName.isPresent()) {
            throw new DataExistsException("This task is already created");
        }

        repo.insert(createTask);
        return createTask;
    }

    public Optional<TaskInputDTO> update(int id, TaskInputDTO task) {
        Optional<TaskOutputDTO> oldTask = repo.findById(id);
        if (oldTask.isEmpty()) {
            throw new DataExistsException("Id " + id + " doesn't exist");
        }

        TaskStatus oldStatus = oldTask.get().getStatus();
        TaskStatus newStatus = task.getStatus();

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException("Invalid status transition");
        }

        if (oldStatus == TaskStatus.COMPLETED) {
            throw new IllegalStateException("This task is already completed");
        }

        repo.setUpdate(task, id);
        return Optional.of(task);
    }

    public boolean delete(int id) {
        return repo.delete(id);
    }
}
