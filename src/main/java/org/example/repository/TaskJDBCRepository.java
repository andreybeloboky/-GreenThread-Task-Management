package org.example.repository;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.example.controller.TaskStatus;
import org.example.exception.DataAccessException;
import org.example.model.Subtask;
import org.example.model.Task;

import java.sql.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Slf4j
public class TaskJDBCRepository {

    private final HikariDataSource dataSource;
    private static final String SELECT_TASK = "SELECT * FROM tasks t ORDER BY t.id";
    private static final String SELECT_SUBTASKS = "SELECT * FROM subtasks s ORDER BY s.id";
    private static final String SELECT_ID = "SELECT * FROM tasks t WHERE id = ? FOR UPDATE";
    private static final String SELECT_ID_SUBTASK = "SELECT * FROM subtasks s WHERE task_id = ? FOR UPDATE";
    private static final String SELECT_TITLE = "SELECT * FROM tasks t WHERE title = ?";
    private static final String SELECT_TITLE_SUBTASK = "SELECT * FROM subtasks t WHERE title = ?";
    private static final String INSERT_TASK = "INSERT INTO tasks (title, description, status, duedate) VALUES (?,?,?,?) RETURNING id";
    private static final String INSERT_SUBTASK = "INSERT INTO subtasks (task_id, title, completed) VALUES (?,?,?) RETURNING id";
    private static final String UPDATE = "UPDATE tasks SET title = ?, description= ?, status= ?, duedate= ? WHERE id = ?";
    private static final String DELETE_TASK = "DELETE FROM tasks WHERE id = ?";
    private static final String DELETE_SUBTASK = "DELETE FROM subtasks WHERE id = ?";

    public TaskJDBCRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ArrayList<Task> getTasksList() {
        ArrayList<Task> list = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_TASK);
             ResultSet rs = preparedStatement.executeQuery()) {
            log.info("Connection opened for take all elements task");
            while (rs.next()) {
                Task obj = getTask(rs);
                list.add(obj);
                log.info("Added an element to list");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public ArrayList<Subtask> getSubtasksList() {
        ArrayList<Subtask> list = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_SUBTASKS);
             ResultSet rs = preparedStatement.executeQuery()) {
            log.info("Connection opened for take all elements task");
            while (rs.next()) {
                Subtask obj = getSubtask(rs);
                list.add(obj);
                log.info("Added an element to list");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public Optional<Task> findByIdTask(int id) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_ID)) {
            preparedStatement.setInt(1, id);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                log.info("Connection opened for find by id element task");
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(getTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Task> findByTitleTask(String title) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_TITLE)) {
            preparedStatement.setString(2, title);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                log.info("Connection opened for find by title element task");
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(getTask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Subtask> findByIdSubtask(int id) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_ID_SUBTASK)) {
            preparedStatement.setInt(1, id);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                log.info("Connection opened for find by title element task");
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(getSubtask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Subtask> findBySubtaskTitle(String title) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_TITLE_SUBTASK)) {
            preparedStatement.setString(1, title);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                log.info("Connection opened for find by title element task");
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(getSubtask(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int insert(Task task) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_TASK)) {
            log.info("Connection opened for insert task");
            bindTaskParams(preparedStatement, task);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Creating failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Incorrect data", e);
        }
    }

    public int insertSubtask(Subtask task) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SUBTASK)) {
            log.info("Connection opened for insert task");
            bindSubtaskParams(preparedStatement, task);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                } else {
                    throw new SQLException("Creating failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            throw new DataAccessException("Incorrect data", e);
        }
    }

    public void setUpdate(Task task, int id) {
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

    public boolean deleteTask(int id) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_TASK)) {
            preparedStatement.setInt(1, id);
            log.info("Connection opened for delete task id={}", id);
            int rows = preparedStatement.executeUpdate();
            log.info("Delete executed for id={}", id);
            return rows != 0;
        } catch (SQLException e) {
            throw new DataAccessException("Element doesn't exist", e);
        }
    }

    public boolean deleteSubtask(int id) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE_SUBTASK)) {
            preparedStatement.setInt(1, id);
            log.info("Connection opened for delete task id={}", id);
            int rows = preparedStatement.executeUpdate();
            log.info("Delete executed for id={}", id);
            return rows != 0;
        } catch (SQLException e) {
            throw new DataAccessException("Element doesn't exist", e);
        }
    }

    private Task getTask(ResultSet rs) throws SQLException {
        Task obj = new Task();
        obj.setId(rs.getInt(1));
        obj.setTitle(rs.getString(2));
        obj.setDescription(rs.getString(3));
        obj.setStatus(TaskStatus.valueOf(rs.getString(4)));
        OffsetDateTime odt = rs.getObject(5, OffsetDateTime.class);
        Instant date = odt.toInstant();
        obj.setDate(date);
        return obj;
    }

    private Subtask getSubtask(ResultSet rs) throws SQLException {
        Subtask obj = new Subtask();
        obj.setId(rs.getInt(1));
        obj.setTask_id(rs.getInt(2));
        obj.setTitle(rs.getString(3));
        obj.setCompleted(rs.getBoolean(4));
        /*OffsetDateTime odt = rs.getObject(5, OffsetDateTime.class);
        Instant date = odt.toInstant();
        obj.setDate(date);
         */
        return obj;
    }

    private void bindTaskParams(PreparedStatement ps, Task task) throws SQLException {
        ps.setString(1, task.getTitle());
        ps.setString(2, task.getDescription());
        ps.setString(3, String.valueOf(Objects.requireNonNullElse(task.getStatus(), "PENDING")));
        OffsetDateTime odt = task.getDate().atOffset(ZoneOffset.UTC);
        ps.setObject(4, odt);
    }

    private void bindSubtaskParams(PreparedStatement ps, Subtask task) throws SQLException {
        ps.setInt(1, task.getTask_id());
        ps.setString(2, task.getTitle());
        ps.setBoolean(3, task.isCompleted());
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