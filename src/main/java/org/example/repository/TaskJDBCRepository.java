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
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
public class TaskJDBCRepository {

    private final HikariDataSource dataSource;
    private static final String SELECT_TASK = "SELECT * FROM tasks t WHERE t.username_id = ? ORDER BY t.id FOR UPDATE";
    private static final String SELECT_SUBTASKS = "SELECT * FROM subtasks s JOIN tasks t ON t.id = s.task_id WHERE username_id =? ORDER BY s.task_id FOR UPDATE";
    private static final String SELECT_ID_TASK = "SELECT * FROM tasks t WHERE id = ? FOR UPDATE";
    private static final String SELECT_ID_SUBTASK = "SELECT * FROM subtasks s JOIN tasks t ON t.id = s.task_id WHERE s.id = ? FOR UPDATE";
    private static final String SELECT_TITLE_TASK = "SELECT * FROM tasks t WHERE title = ? FOR UPDATE";
    private static final String SELECT_TITLE_SUBTASK = "SELECT * FROM subtasks s JOIN tasks t ON t.id = s.task_id WHERE s.title = ? FOR UPDATE";
    private static final String INSERT_TASK = "INSERT INTO tasks (title, description, status, duedate, username_id) VALUES (?,?,?,?,?) RETURNING id";
    private static final String INSERT_SUBTASK = "INSERT INTO subtasks (task_id, title, completed) VALUES (?,?,?) RETURNING id";
    private static final String UPDATE_TASK = "UPDATE tasks SET title = ?, description= ?, status= ?, duedate= ? WHERE id = ?";
    private static final String UPDATE_SUBTASK = "UPDATE subtasks SET task_id = ?, title = ?, completed= ? WHERE id = ?";
    private static final String DELETE_TASK = "DELETE FROM tasks WHERE id = ?";
    private static final String DELETE_SUBTASK = "DELETE FROM subtasks WHERE id = ?";

    public TaskJDBCRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ArrayList<Task> loadTasksList(int userId) {
        return queryList(SELECT_TASK, this::loadTask, userId);
    }

    public ArrayList<Subtask> loadSubtasksList(int userId) {
        return queryList(SELECT_SUBTASKS, this::loadSubtask, userId);
    }

    public Optional<Task> findByTitleTask(String title) {
        return find(SELECT_TITLE_TASK, title, this::loadTask);
    }

    public Optional<Subtask> findByTitleSubtask(String title) {
        return find(SELECT_TITLE_SUBTASK, title, this::loadSubtask);
    }

    public Optional<Task> findByIdTask(int id) {
        return find(SELECT_ID_TASK, id, this::loadTask);
    }

    public Optional<Subtask> findByIdSubtask(int id) {
        return find(SELECT_ID_SUBTASK, id, this::loadSubtask);
    }

    public int insertTask(Task task) {
        return executeInsert(INSERT_TASK, ps -> bindTaskParams(ps, task));
    }

    public int insertSubtask(Subtask subtask) {
        return executeInsert(INSERT_SUBTASK, ps -> bindSubtaskParams(ps, subtask));
    }

    public void updateTask(Task task, int id) {
        executeUpdate(UPDATE_TASK, id, preparedStatement -> bindTaskParams(preparedStatement, task), 5);
    }

    public void updateSubtask(Subtask subtask, int id) {
        executeUpdate(UPDATE_SUBTASK, id, preparedStatement -> bindSubtaskParams(preparedStatement, subtask), 4);
    }

    public void deleteTask(int id) {
        deleteById(DELETE_TASK, id);
    }

    public void deleteSubtask(int id) {
        deleteById(DELETE_SUBTASK, id);
    }

    private <T> ArrayList<T> queryList(String sql, Function<ResultSet, T> mapper, int userId) {
        ArrayList<T> list = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                log.info("Connection opened for query: {}", sql);
                while (rs.next()) {
                    list.add(mapper.apply(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    private void deleteById(String query, int id) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {
            preparedStatement.setInt(1, id);
            log.info("Connection opened for delete task id={}", id);
            preparedStatement.executeUpdate();
            log.info("Delete executed for id={}", id);
        } catch (SQLException e) {
            throw new DataAccessException("Element doesn't exist", e);
        }
    }

    private <T> Optional<T> find(String sql, Object param, Function<ResultSet, T> mapper) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
            preparedStatement.setObject(1, param);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                log.info("Connection opened for find by id element task");
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapper.apply(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private int executeInsert(String sql, Consumer<PreparedStatement> binder) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            log.info("Connection opened for insert task");
            binder.accept(preparedStatement);
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

    private void executeUpdate(String sql, int id, Consumer<PreparedStatement> binder, int idParam) {
        Connection connection = null;
        try {
            connection = openConnection();
            log.info("Connection opened for update task id={}", id);
            connection.setAutoCommit(false);
            log.debug("Transaction started (autoCommit=false)");

            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                binder.accept(ps);
                ps.setInt(idParam, id);
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

    private Task loadTask(ResultSet rs) {
        try {
            Task obj = new Task();
            obj.setId(rs.getInt(1));
            obj.setTitle(rs.getString(2));
            obj.setDescription(rs.getString(3));
            obj.setStatus(TaskStatus.valueOf(rs.getString(4)));
            OffsetDateTime odt = rs.getObject(5, OffsetDateTime.class);
            Instant date = odt.toInstant();
            obj.setDate(date);
            obj.setUsernameId(6);
            return obj;
        } catch (SQLException e) {
            throw new DataAccessException("Could not bind parameters for Task", e);
        }
    }

    private Subtask loadSubtask(ResultSet rs) {
        try {
            Subtask obj = new Subtask();
            obj.setId(rs.getInt(1));
            obj.setTaskId(rs.getInt(2));
            obj.setTitle(rs.getString(3));
            obj.setCompleted(rs.getBoolean(4));
            OffsetDateTime odt = rs.getObject(5, OffsetDateTime.class);
            Instant date = odt.toInstant();
            obj.setCreated_at(date);
            return obj;
        } catch (SQLException e) {
            throw new DataAccessException("Could not bind parameters for Task", e);
        }
    }

    private void bindTaskParams(PreparedStatement ps, Task task) {
        try {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, String.valueOf(Objects.requireNonNullElse(task.getStatus(), "PENDING")));
            OffsetDateTime odt = task.getDate().atOffset(ZoneOffset.UTC);
            ps.setObject(4, odt);
            ps.setInt(5, task.getUsernameId());
        } catch (SQLException e) {
            throw new DataAccessException("Could not bind parameters for Task", e);
        }
    }

    private void bindSubtaskParams(PreparedStatement ps, Subtask task) {
        try {
            ps.setInt(1, task.getTaskId());
            ps.setString(2, task.getTitle());
            ps.setBoolean(3, task.isCompleted());
        } catch (SQLException e) {
            throw new DataAccessException("Could not bind parameters for Task", e);
        }
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