package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.dto.TaskInputDTO;
import org.example.controller.TaskStatus;
import org.example.dto.TaskOutputDTO;
import org.example.exception.DataExistsException;
import org.example.exception.InvalidStatusTransitionException;
import org.example.repository.TaskJDBCRepository;

import java.util.ArrayList;
import java.util.Optional;


public class TaskService {

    private final TaskJDBCRepository repo;

    public TaskService(HikariDataSource ds) {
        this.repo = new TaskJDBCRepository(ds);
    }

    public ArrayList<TaskOutputDTO> takeAllElements() {
        return repo.getList();
    }

    public TaskInputDTO createTask(TaskInputDTO createTask) {
        Optional<TaskOutputDTO> theSameTitleName = repo.findByTitle(createTask.getTitle());

        if (theSameTitleName.isPresent()) {
            throw new DataExistsException("This task is already created");
        }

        int id = repo.insert(createTask);
        createTask.setId(id);
        return createTask;
    }

    public TaskInputDTO update(int id, TaskInputDTO task) {
        Optional<TaskOutputDTO> oldTask = repo.findById(id);
        if (oldTask.isEmpty()) {
            throw new DataExistsException("Id " + id + " doesn't exist");
        }

        TaskStatus oldStatus = oldTask.get().getStatus();
        TaskStatus newStatus = task.getStatus();

        if (oldStatus == TaskStatus.COMPLETED) {
            throw new InvalidStatusTransitionException("This task is already completed");
        }

        if (!oldStatus.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException("Invalid status transition");
        }

        repo.setUpdate(task, id);
        return task;
    }

    public boolean delete(int id) {
        return repo.delete(id);
    }
}
