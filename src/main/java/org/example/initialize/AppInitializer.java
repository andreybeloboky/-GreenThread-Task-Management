package org.example.initialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Task;
import org.example.service.TaskService;

@WebListener
@Slf4j
public class AppInitializer implements ServletContextListener {

    private HikariDataSource dataSource;
    private static final String URL = System.getenv("DB_URL_TASK");
    private static final String LOGIN = System.getenv("DB_LOGIN");
    private static final String PASSWORD = System.getenv("DB_PASSWORD_TASK");
    private ValidatorFactory factory;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.debug("dataSource works");
        HikariConfig configHikari = new HikariConfig();
        configHikari.setJdbcUrl(URL);
        configHikari.setUsername(LOGIN);
        configHikari.setPassword(PASSWORD);
        configHikari.setMaximumPoolSize(10);
        configHikari.setDriverClassName("org.postgresql.Driver");
        dataSource = new HikariDataSource(configHikari);
        sce.getServletContext().setAttribute("datasource", dataSource);

        factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        sce.getServletContext().setAttribute("validator", validator);

        TaskService service = new TaskService(dataSource);
        sce.getServletContext().setAttribute("service", service);

        ObjectMapper mapper = new ObjectMapper();
        sce.getServletContext().setAttribute("mapper", mapper);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dataSource != null) {
            dataSource.close();
        }
        if (factory != null) {
            factory.close();
        }
    }
}
