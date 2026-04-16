package org.example.initialize;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@WebListener
public class ValidationInitializer implements ServletContextListener {

    private ValidatorFactory factory;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        sce.getServletContext().setAttribute("validator", validator);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (factory != null) {
            factory.close();
        }
    }
}
