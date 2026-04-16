package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.DTO.TaskDTO;
import org.example.repository.JDBCRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class TaskService {

    private final JDBCRepository repo;

    public TaskService(HikariDataSource ds) {
        this.repo = new JDBCRepository(ds);
    }

    public ArrayList<TaskDTO> takeAllElements() {
        return repo.getList();
    }

    public boolean createTask(String title, String description, String status, String dueDate) {
        LocalDateTime parsedDateTime = LocalDateTime.parse(dueDate);
        LocalDateTime now = LocalDateTime.now();
        if (title.length() < 5 || title.length() > 100 || description.length() > 500 || !parsedDateTime.isAfter(now)) {
            return false;
        }
        this.repo.insert(title, description, status, parsedDateTime);
        return true;
    }

    public void update() {
        this.repo.setUpdate();
    }

    public boolean delete(int id) {
        return this.repo.delete(id);
    }
}
