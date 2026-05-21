package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.dto.LoginRequest;
import org.example.dto.SubtaskRequest;
import org.example.dto.TaskRequest;
import org.example.controller.TaskStatus;
import org.example.exception.DataExistsException;
import org.example.exception.InvalidStatusTransitionException;
import org.example.model.Login;
import org.example.model.Subtask;
import org.example.model.Task;
import org.example.repository.TaskJDBCRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;


public class TaskService {

    private final TaskJDBCRepository repo;

    public TaskService(HikariDataSource ds) {
        this.repo = new TaskJDBCRepository(ds);
    }

    public ArrayList<Task> findAllTask(int userId) {
        ArrayList<Task> tasks = repo.loadTasksList(userId);
        ArrayList<Subtask> subtasks = repo.loadSubtasksList();
        for (Task task : tasks) {
            List<Subtask> related = subtasks.stream()
                    .filter(s -> s.getTaskId() == task.getId())
                    .collect(Collectors.toList());
            task.setSubtasks(related);
        }
        return tasks;
    }

    public ArrayList<Subtask> findAllSubtask() {
        return repo.loadSubtasksList();
    }

    public Task create(TaskRequest createTask, int userId) {
        Task task = inizilizeTask(createTask, userId);
        Optional<Task> theSameTitleName = repo.findByTitleTask(task.getTitle());
        if (theSameTitleName.isPresent()) {
            if (theSameTitleName.get().getUsernameId() == task.getUsernameId()) {
                throw new DataExistsException("This task is already created");
            }
        }
        int id = repo.insertTask(task);
        task.setId(id);
        return task;
    }

    public Subtask createSubtask(SubtaskRequest subtaskRequest) {
        Subtask subtask = inizilizeSubtask(subtaskRequest);
        Optional<Subtask> theSameTitleName = repo.findByTitleSubtask(subtask.getTitle());

        if (theSameTitleName.isPresent()) {
            throw new DataExistsException("This subtask is already created");
        }

        int id = repo.insertSubtask(subtask);
        subtask.setId(id);
        return subtask;
    }

    public Task update(int id, TaskRequest taskUpdate, int userId) {
        Task task = inizilizeTask(taskUpdate, userId);
        Optional<Task> oldTask = repo.findByIdTask(id);
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

        repo.setUpdateTask(task, id);
        task.setId(id);
        return task;
    }

    public Subtask updateSubtask(int id, SubtaskRequest taskUpdate) {
        Subtask subtask = inizilizeSubtask(taskUpdate);
        Optional<Subtask> oldTask = repo.findByIdSubtask(id);
        if (oldTask.isEmpty()) {
            throw new DataExistsException("Id " + id + " doesn't exist");
        }
        repo.updateSubtask(subtask, id);
        subtask.setId(id);
        return subtask;
    }

    public void deleteTask(int id) {
        deleteEntity(id, repo::findByIdTask, repo::deleteTask, "This task doesn't exist");
    }

    public void deleteSubtask(int id) {
        deleteEntity(id, repo::findByIdSubtask, repo::deleteSubtask, "This subtask doesn't exist");
    }

    private <T> void deleteEntity(int id, Function<Integer, Optional<T>> finder, Consumer<Integer> deleter, String notFoundMessage) {
        if (finder.apply(id).isEmpty()) {
            throw new DataExistsException(notFoundMessage);
        }
        deleter.accept(id);
    }

    private Subtask inizilizeSubtask(SubtaskRequest createTask) {
        Subtask task = new Subtask();
        task.setTaskId(createTask.getTask_id());
        task.setTitle(createTask.getTitle());
        task.setCompleted(createTask.isCompleted());
        return task;
    }

    private Task inizilizeTask(TaskRequest createTask, int userId) {
        Task task = new Task();
        task.setId(createTask.getId());
        task.setTitle(createTask.getTitle());
        task.setDescription(createTask.getDescription());
        task.setDate(createTask.getDate());
        task.setStatus(createTask.getStatus());
        task.setUsernameId(userId);
        return task;
    }
}
