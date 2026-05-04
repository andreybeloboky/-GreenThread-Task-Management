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
        setJsonHeaders(resp);
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
            setJsonHeaders(resp);
            mapper.writeValue(resp.getWriter(), create);
            String appName = req.getContextPath();
            resp.setHeader("Location", appName + "/" + create.getId());
        } catch (DataExistsException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write(e.getMessage());
        } catch (ValidationException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write(e.getMessage());
        } catch (JsonProcessingException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);

            Map<String, String> errorBody = new HashMap<>();
            errorBody.put("error", "Invalid JSON format");
            errorBody.put("details", e.getOriginalMessage());

            mapper.writeValue(resp.getWriter(), errorBody);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TaskInputDTO updateDate = mapper.readValue(req.getInputStream(), TaskInputDTO.class);
        int idParam = Integer.parseInt(req.getParameter("id"));
        if (idParam < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter" + idParam);
            return;
        }
        setJsonHeaders(resp);
        try {
            validateData(updateDate);
            TaskInputDTO updateData = task.update(idParam, updateDate);
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), updateData);
        } catch (DataExistsException | InvalidStatusTransitionException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            resp.getWriter().write(e.getMessage());
        } catch (ValidationException e) {
            setJsonHeaders(resp);
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
        task.delete(idParam);
        resp.setStatus(HttpServletResponse.SC_NO_CONTENT);

    }

    @Override
    public void destroy() {
        ds.close();
    }

    private void validateData(TaskInputDTO task) throws JsonProcessingException {
        Set<ConstraintViolation<TaskInputDTO>> violations = val.validate(task);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<TaskInputDTO> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("errors", errors);
            throw new ValidationException(mapper.writeValueAsString(responseBody));
        }
    }

    private void setJsonHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
    }
}
