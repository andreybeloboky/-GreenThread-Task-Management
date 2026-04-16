package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.DTO.TaskDTO;
import org.example.repository.JDBCRepository;

import java.util.ArrayList;

public class TaskService {

    private final JDBCRepository repo;

    public TaskService(HikariDataSource ds) {
        this.repo = new JDBCRepository(ds);
    }

    public ArrayList<TaskDTO> takeAllElements() {
        return repo.getList();
    }

    public boolean createTask(String title, String description, String dueDate) {
        if(title.length() < 5 || title.length() > 100){
            return false;
        }
        if(description.length()>500){
            return false;
        }
        this.repo.insert();
        return true;
    }

    public void update() {
        this.repo.setUpdate();
    }

    public boolean delete(int id){
        return this.repo.delete(id);
    }
}
