package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.example.DTO.TaskDTO;
import org.example.repository.JDBCRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Set;

public class TaskService {

    private final JDBCRepository repo;
    private final Validator validator;

    public TaskService(HikariDataSource ds, Validator validatorIn) {
        this.repo = new JDBCRepository(ds);
        this.validator = validatorIn;
    }

    public ArrayList<TaskDTO> takeAllElements() {
        return repo.getList();
    }

    public boolean createTask(String title, String description, String status, String dueDate) {
        TaskDTO dto = new TaskDTO();
        dto.setTitle(title);
        dto.setDescription(description);
        dto.setDate(LocalDateTime.parse(dueDate));
        dto.setStatus(status);
        Set<ConstraintViolation<TaskDTO>> violations = validator.validate(dto);
        if (!violations.isEmpty()) {
            return false;
        }
        repo.insert(dto);
        return true;
    }

    public boolean update(int id) {
        TaskDTO task = repo.getTask(id);
        this.repo.setUpdate();
        return true;
    }

    public boolean delete(int id) {
        return this.repo.delete(id);
    }
}
