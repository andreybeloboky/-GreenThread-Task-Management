package org.example.initialize;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@WebListener
public class AppInitializer implements ServletContextListener {

    private HikariDataSource dataSource;
    private static final String URL = System.getenv("DB_URL_TASK");
    private static final String LOGIN = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD_TASK");
    private ValidatorFactory factory;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(URL);
        config.setUsername(LOGIN);
        config.setPassword(PASSWORD);
        config.setMaximumPoolSize(10);
        config.setDriverClassName("org.postgresql.Driver");
        dataSource = new HikariDataSource(config);
        sce.getServletContext().setAttribute("datasource", dataSource);
        factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        sce.getServletContext().setAttribute("validator", validator);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dataSource != null) {
            dataSource.close();
            System.out.println("HikariCP closed");
        }
        if (factory != null) {
            factory.close();
        }
    }
}
