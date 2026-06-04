package org.example.repository;

import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.DataAccessException;
import org.example.exception.DataConflictException;
import org.example.model.Login;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Slf4j
public class AuthRepo {

    private static final String SELECT_USERNAME = "SELECT * FROM users WHERE username =?";
    private final HikariDataSource dataSource;

    public AuthRepo(HikariDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Login verify(String username, String password) {
        Login user = new Login();
        try (Connection conn = openConnection();
             PreparedStatement preparedStatement = conn.prepareStatement(SELECT_USERNAME)) {
            preparedStatement.setString(1, username);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                rs.next();
                user.setLogin(rs.getString(2));
                user.setPassword(rs.getString(3));
                if (!user.getLogin().equals(username) || !user.getPassword().equals(password)) {
                    throw new DataConflictException("Invalid login/password");
                }
                user.setId(rs.getInt(1));
                return user;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Invalid login/password", e);
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
