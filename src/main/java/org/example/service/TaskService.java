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

    public Task create(TaskRequest createTask) {
        Task task = inizilizeTask(createTask);
        Optional<Task> theSameTitleName = repo.findByTitle(task.getTitle());

        if (theSameTitleName.isPresent()) {
            throw new DataExistsException("This task is already created");
        }

        int id = repo.insert(task);
        task.setId(id);
        return task;
    }

    public Task update(int id, TaskRequest taskUpdate) {
        Task task = inizilizeTask(taskUpdate);
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
        task.setId(id);
        return task;
    }

    public void delete(int id) {
        Optional<Subtask> task = repo.findByTitleSubtask(id);
        if (task.isPresent()) {
            throw new DataExistsException("This task includes subtasks. You can't delete it");
        } else {
            repo.delete(id);
        }
    }


    private Task inizilizeTask(TaskRequest createTask) {
        Task task = new Task();
        task.setId(createTask.getId());
        task.setTitle(createTask.getTitle());
        task.setDescription(createTask.getDescription());
        task.setDate(createTask.getDate());
        task.setStatus(createTask.getStatus());
        return task;
    }
}
