package org.example.repository;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.TaskInputDTO;
import org.example.controller.TaskStatus;
import org.example.dto.TaskOutputDTO;
import org.example.exception.DataAccessException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
public class TaskJDBCRepository {

    private final HikariDataSource dataSource;
    private static final String SELECT = "SELECT * FROM tasks t ORDER BY t.id";
    private static final String SELECT_ID = "SELECT * FROM tasks t WHERE id = ? FOR UPDATE";
    private static final String SELECT_TITLE = "SELECT * FROM tasks t WHERE title = ?";
    private static final String INSERT = "INSERT INTO tasks (title, description, status, duedate) VALUES (?,?,?,?)";
    private static final String UPDATE = "UPDATE tasks SET title = ?, description= ?, status= ?, duedate= ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM tasks WHERE id = ?";

    public TaskJDBCRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ArrayList<TaskOutputDTO> getList() {
        ArrayList<TaskOutputDTO> list = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT);
             ResultSet rs = preparedStatement.executeQuery()) {
            log.info("Connection opened for take all elements task");
            while (rs.next()) {
                TaskOutputDTO obj = getElement(rs);
                list.add(obj);
                log.info("Added an element to list");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Optional<TaskOutputDTO> findById(int id) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_ID)) {
            preparedStatement.setInt(1, id);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                log.info("Connection opened for find by id element task");
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(getElement(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<TaskOutputDTO> findByTitle(String title) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_TITLE)) {
            preparedStatement.setString(1, title);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                log.info("Connection opened for find by title element task");
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(getElement(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TaskOutputDTO getElement(ResultSet rs) throws SQLException {
        TaskOutputDTO obj = new TaskOutputDTO();
        obj.setTitle(rs.getString(2));
        obj.setDescription(rs.getString(3));
        obj.setStatus(TaskStatus.valueOf(rs.getString(4)));
        Timestamp ts = rs.getTimestamp(5);
        LocalDateTime localDate = ts.toLocalDateTime();
        obj.setDate(localDate);
        return obj;
    }


    public void insert(TaskInputDTO task) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT)) {
            log.info("Connection opened for insert task");
            bindTaskParams(preparedStatement, task);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Incorrect data", e);
        }
    }

    public void setUpdate(TaskInputDTO task, int id) {
        Connection connection = null;
        try {
            connection = openConnection();
            log.info("Connection opened for update task id={}", id);
            connection.setAutoCommit(false);
            log.debug("Transaction started (autoCommit=false)");

            try (PreparedStatement ps = connection.prepareStatement(UPDATE)) {
                bindTaskParams(ps, task);
                ps.setInt(5, id);
                int rows = ps.executeUpdate();
                log.info("Update executed for id={}, affected rows={}", id, rows);
                connection.commit();
                log.debug("Transaction committed successfully");
            }
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    connection.rollback();
                    log.warn("Transaction rolled back due to error: {}", e.getMessage());
                } catch (SQLException ex) {
                    log.error("Rollback failed: {}", ex.getMessage(), ex);
                }
            }
            throw new DataAccessException("Update failed", e);
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                    log.debug("Connection closed for update task id={}", id);
                } catch (SQLException ex) {
                    log.error("Failed to close connection: {}", ex.getMessage(), ex);
                }
            }
        }
    }


    public boolean delete(int id) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE)) {
            preparedStatement.setInt(1, id);
            log.info("Connection opened for delete task id={}", id);
            int rows = preparedStatement.executeUpdate();
            log.info("Delete executed for id={}", id);
            return rows != 0;
        } catch (SQLException e) {
            throw new DataAccessException("Element doesn't exist", e);
        }
    }

    private void bindTaskParams(PreparedStatement ps, TaskInputDTO task) throws SQLException {
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setString(3, String.valueOf(Objects.requireNonNullElse(task.getStatus(), "PENDING")));
        ps.setObject(4, task.getDate());
    }


    private Connection openConnection() throws SQLException {
        try {
            log.info("Opening database connection");
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.warn("Unable to establish database connection", e);
            throw new DataAccessException("Impossible connect with database", e);
        }
    }
}