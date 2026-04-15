package org.example.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.example.DTO.TaskDTO;
import org.example.exception.DataAccessException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

@Slf4j
public class JDBCRepository {

    private final HikariDataSource dataSource;
    private static final String URL = System.getenv("DB_URL_TASK");
    private static final String LOGIN = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD_TASK");
    private static final String SELECT = "SELECT * FROM tasks t ORDER BY t.id";
    private static final String INSERT = "INSERT INTO tasks (title, description, status, duedate) VALUES (?,?,?,?)";
    private static final String UPDATE = "UPDATE tasks SET status = ? WHERE id = ?";
    private static final String DELETE = "DELETE FROM tasks WHERE id = ?";

    public JDBCRepository() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(LOGIN);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setDriverClassName("org.postgresql.Driver");
        dataSource = new HikariDataSource(config);
    }

    public ArrayList<TaskDTO> getList() {
        ArrayList<TaskDTO> list = new ArrayList<>();
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT);
             ResultSet rs = preparedStatement.executeQuery()) {
            while (rs.next()) {
                TaskDTO obj = new TaskDTO();
                obj.setTitle(rs.getString(2));
                obj.setDescription(rs.getString(3));
                obj.setStatus(rs.getString(4));
                obj.setDate(rs.getDate(5));
                list.add(obj);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public void insert() {
        try (Connection connection = openConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(INSERT)) {
            preparedStatement.setString(1, "Task 4");
            preparedStatement.setString(2, "Nothing");
            preparedStatement.setString(3, "PENDING");
            LocalDateTime dateTime = LocalDateTime.of(2026, 4, 17, 14, 30);
            preparedStatement.setTimestamp(4, Timestamp.valueOf(dateTime));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("crush", e);
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



