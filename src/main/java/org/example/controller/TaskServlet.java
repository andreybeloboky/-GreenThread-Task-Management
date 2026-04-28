package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.example.dto.TaskInputDTO;
import org.example.dto.TaskOutputDTO;
import org.example.exception.DataExistsException;
import org.example.exception.InvalidStatusTransitionException;
import org.example.service.TaskService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {

    private TaskService task;
    private final ObjectMapper mapper = new ObjectMapper();
    private Validator val;
    private HikariDataSource ds;
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String ENCODING_UTF8 = "UTF-8";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            Object dsObj = getServletContext().getAttribute("datasource");
            Object valObj = getServletContext().getAttribute("validator");
            if (!(dsObj instanceof HikariDataSource) || !(valObj instanceof Validator)) {
                throw new ServletException("Required context attributes 'datasource' or 'validator' are missing or invalid");
            }

            ds = (HikariDataSource) dsObj;
            val = (Validator) valObj;
            task = new TaskService(ds);
        } catch (Exception e) {
            throw new ServletException("Failed to initialize the library", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ArrayList<TaskOutputDTO> data = task.takeAllElements();
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(ENCODING_UTF8);
        if (data == null || data.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), new ArrayList<>());
            return;
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        mapper.writeValue(resp.getWriter(), data);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            TaskInputDTO createTask = mapper.readValue(req.getInputStream(), TaskInputDTO.class);
            validateData(createTask);
            TaskInputDTO create = task.createTask(createTask);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType(CONTENT_TYPE_JSON);
            resp.setCharacterEncoding(ENCODING_UTF8);
            mapper.writeValue(resp.getWriter(), create);

        } catch (DataExistsException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write(e.getMessage());
        } catch (JsonProcessingException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\":\"Invalid JSON format\"}");
        } catch (ValidationException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TaskInputDTO updateDate = mapper.readValue(req.getInputStream(), TaskInputDTO.class);
        int id = Integer.parseInt(req.getParameter("id"));
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(ENCODING_UTF8);
        try {
            validateData(updateDate);
            TaskInputDTO updateData = task.update(id, updateDate);
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), updateData);
        } catch (DataExistsException | InvalidStatusTransitionException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write(e.getMessage());
        } catch (ValidationException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int idParam = Integer.parseInt(req.getParameter("id"));
        if (idParam < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter" + idParam);
            return;
        }
        boolean deleted = task.delete(idParam);
        if (deleted) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Task is not found");
            return;
        }
    }

    private void validateData(TaskInputDTO task) throws JsonProcessingException {
        Set<ConstraintViolation<TaskInputDTO>> violations = val.validate(task);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<TaskInputDTO> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new ValidationException(mapper.writeValueAsString(errors));
        }
    }
}
