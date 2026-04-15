package org.example.service;

import org.example.DTO.TaskDTO;
import org.example.repository.JDBCRepository;

import java.util.ArrayList;

public class TaskService {

    private JDBCRepository JDBCRepository;

    public TaskService() {
        this.JDBCRepository = new JDBCRepository();
    }

    public ArrayList<TaskDTO> takeAllElements() {
        return JDBCRepository.getList();
    }

    public void createTask() {
        this.JDBCRepository.insert();
    }
}
