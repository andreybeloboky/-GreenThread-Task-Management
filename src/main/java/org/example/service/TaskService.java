package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.dto.SubtaskRequest;
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
        Optional<Task> theSameTitleName = repo.findByTitleTask(task.getTitle());

        if (theSameTitleName.isPresent()) {
            throw new DataExistsException("This task is already created");
        }

        int id = repo.insertTask(task);
        task.setId(id);
        return task;
    }

    public Subtask createSubtask(SubtaskRequest createTask) {
        Subtask task = inizilizeSubtask(createTask);
        Optional<Subtask> theSameTitleName = repo.findByTitleSubtask(task.getTitle());

        if (theSameTitleName.isPresent()) {
            throw new DataExistsException("This task is already created");
        }

        int id = repo.insertSubtask(task);
        task.setId(id);
        return task;
    }

    public Task update(int id, TaskRequest taskUpdate) {
        Task task = inizilizeTask(taskUpdate);
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
        Subtask task = inizilizeSubtask(taskUpdate);
        Optional<Subtask> oldTask = repo.findByIdSubtask(id);
        if (oldTask.isEmpty()) {
            throw new DataExistsException("Id " + id + " doesn't exist");
        }
        repo.setUpdateSubtask(task, id);
        task.setId(id);
        return task;
    }

    public void deleteTask(int id) {
        Optional<Task> task = repo.findByIdTask(id);
        if (task.isPresent()) {
            throw new DataExistsException("This task includes subtasks. You can't delete it");
        } else {
            repo.deleteTask(id);
        }
    }

    public void deleteSubtask(int id) {
        Optional<Subtask> task = repo.findByIdSubtask(id);
        if (task.isEmpty()) {
            throw new DataExistsException("This task doesn't exist");
        } else {
            repo.deleteSubtask(id);
        }
    }

    private Subtask inizilizeSubtask(SubtaskRequest createTask) {
        Subtask task = new Subtask();
        task.setTask_id(createTask.getTask_id());
        task.setTitle(createTask.getTitle());
        task.setCompleted(createTask.isCompleted());
        return task;
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
