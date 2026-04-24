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
import org.example.service.TaskService;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String ENCODING_UTF8 = "UTF-8";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            HikariDataSource ds = (HikariDataSource) getServletContext().getAttribute("datasource");
            val = (Validator) getServletContext().getAttribute("validator");
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
        mapper.writeValue(resp.getWriter(), data);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TaskInputDTO createTask = mapper.readValue(req.getInputStream(), TaskInputDTO.class);
        try {
            validate(createTask);
            TaskInputDTO create = task.createTask(createTask);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType(CONTENT_TYPE_JSON);
            resp.setCharacterEncoding(ENCODING_UTF8);
            mapper.writeValue(resp.getWriter(), create);

        } catch (ValidationException | DataExistsException e) {
            resp.getWriter().write(e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TaskInputDTO updateDate = mapper.readValue(req.getInputStream(), TaskInputDTO.class);
        int id = Integer.parseInt(req.getParameter("id"));
        try {
            validate(updateDate);

            TaskInputDTO updateData = task.update(id, updateDate);
            resp.setContentType(CONTENT_TYPE_JSON);
            resp.setCharacterEncoding(ENCODING_UTF8);
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), updateData);
        } catch (ValidationException | DataExistsException e) {
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
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().write("The data of id" + idParam + " is deleted successfully");
        } else {
            resp.sendError(HttpServletResponse.SC_CONFLICT, "Task is not found");
        }
    }

    private void validate(TaskInputDTO up) throws JsonProcessingException {
        Set<ConstraintViolation<TaskInputDTO>> violations = val.validate(up);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<TaskInputDTO> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            throw new ValidationException(mapper.writeValueAsString(errors));
        }
    }
}
