package org.example.repository;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.example.DTO.TaskDTO;
import org.example.exception.DataAccessException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;

@Slf4j
public class JDBCRepository {

    private final HikariDataSource dataSource;
    private static final String SELECT = "SELECT * FROM tasks t ORDER BY t.id";
    private static final String SELECT_ID = "SELECT * FROM tasks t WHERE id = ?";
    private static final String INSERT = "INSERT INTO tasks (title, description, status, duedate) VALUES (?,?,?,?)";
    private static final String UPDATE = "UPDATE tasks SET status = ? WHERE id = ? FOR UPDATE";
    private static final String DELETE = "DELETE FROM tasks WHERE id = ?";

    public JDBCRepository(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public ArrayList<TaskDTO> getList() {
        ArrayList<TaskDTO> list = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT);
             ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                TaskDTO obj = getElement(rs);
                list.add(obj);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public TaskDTO getTask(int id) {
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_ID)) {
            preparedStatement.setInt(1, id);
            ResultSet rs = preparedStatement.executeQuery();
            rs.next();
            return getElement(rs);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private TaskDTO getElement(ResultSet rs) throws SQLException {
        TaskDTO obj = new TaskDTO();
        obj.setTitle(rs.getString(2));
        obj.setDescription(rs.getString(3));
        obj.setStatus(rs.getString(4));
        Timestamp ts = rs.getTimestamp(5);
        LocalDateTime localDate = ts.toLocalDateTime();
        obj.setDate(localDate);
        return obj;
    }


    public void insert(TaskDTO task) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT)) {
            preparedStatement.setString(1, task.getTitle());
            preparedStatement.setString(2, task.getDescription());
            preparedStatement.setString(3, Objects.requireNonNullElse(task.getStatus(), "PENDING"));
            preparedStatement.setTimestamp(4, Timestamp.valueOf(task.getDate()));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Incorrect data", e);
        }
    }

    public void setUpdate() {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(UPDATE)) {
            preparedStatement.setString(1, "COMPLETED");
            preparedStatement.setInt(2, 1);
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("crush", e);
        }
    }

    public boolean delete(int id) {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(DELETE)) {
            preparedStatement.setInt(1, id);
            int rows = preparedStatement.executeUpdate();
            return rows != 0;
        } catch (SQLException e) {
            throw new DataAccessException("crush", e);
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



