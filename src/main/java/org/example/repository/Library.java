package org.example.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.example.exception.DataAccessException;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
public class Library {

    private static final HikariDataSource dataSource;
    private static final String URL = System.getenv("DB_URL");
    private static final String LOGIN = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");



    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setPassword(LOGIN);
        config.setUsername(PASSWORD);
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    private static Connection openConnection() throws SQLException {
        try {
            log.info("Opening database connection");
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.warn("Unable to establish database connection", e);
            throw new DataAccessException("Impossible connect with database", e);
        }
    }

}



