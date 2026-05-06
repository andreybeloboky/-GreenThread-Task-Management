package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.dto.TaskRequest;
import org.example.controller.TaskStatus;
import org.example.exception.DataExistsException;
import org.example.exception.InvalidStatusTransitionException;
import org.example.model.Subtask;
import org.example.model.Task;
import org.example.repository.TaskJDBCRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;


public class TaskService {

    private final TaskJDBCRepository repo;

    public TaskService(HikariDataSource ds) {
        this.repo = new TaskJDBCRepository(ds);
    }

    public ArrayList<Task> findAllTask() {
        ArrayList<Task> tasks = repo.getTasksList();
        ArrayList<Subtask> subtasks = repo.getSubtasksList();


        for (Task task : tasks) {
            List<Subtask> related = subtasks.stream()
                    .filter(s -> s.getTask_id() == task.getId())
                    .collect(Collectors.toList());
            task.setSubtasks(related);
        }

        return tasks;
    }

    public ArrayList<Subtask> findAllSubtask() {
        return repo.getSubtasksList();
    }

    public TaskRequest create(TaskRequest createTask) {
        Optional<Task> theSameTitleName = repo.findByTitle(createTask.getTitle());

        if (theSameTitleName.isPresent()) {
            throw new DataExistsException("This task is already created");
        }

        int id = repo.insert(createTask);
        createTask.setId(id);
        return createTask;
    }

    public TaskRequest update(int id, TaskRequest task) {
        Optional<Task> oldTask = repo.findById(id);
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

    public void delete(int id) {
        repo.delete(id);
    }
}
