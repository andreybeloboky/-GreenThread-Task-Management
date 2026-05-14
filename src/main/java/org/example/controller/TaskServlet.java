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
import org.example.dto.TaskRequest;
import org.example.dto.TaskResponse;
import org.example.exception.DataExistsException;
import org.example.exception.InvalidStatusTransitionException;
import org.example.model.Task;
import org.example.service.TaskService;

import java.io.IOException;
import java.util.*;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {

    private TaskService service;
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
            val = (Validator) getServletContext().getAttribute("validator");
            service = (TaskService) getServletContext().getAttribute("service");
        } catch (Exception e) {
            throw new ServletException("Failed to initialize the library", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int userId = (Integer) req.getSession().getAttribute("id");
        ArrayList<Task> data = service.findAllTask(userId);
        setJsonHeaders(resp);
        if (data == null || data.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), new ArrayList<>());
            return;
        }
        List<TaskResponse> dtoList = data.stream()
                .map(TaskResponse::new)
                .toList();

        resp.setStatus(HttpServletResponse.SC_OK);
        mapper.writeValue(resp.getWriter(), dtoList);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            TaskRequest createTask = mapper.readValue(req.getInputStream(), TaskRequest.class);
            validateData(createTask);
            int userId = (Integer) req.getSession().getAttribute("id");
            Task create = service.create(createTask, userId);
            TaskResponse taskResponse = new TaskResponse(create);

            resp.setStatus(HttpServletResponse.SC_CREATED);
            setJsonHeaders(resp);
            mapper.writeValue(resp.getWriter(), taskResponse);
            String appName = req.getContextPath();
            resp.setHeader("Location", appName + "/tasks/" + taskResponse.getId());
        } catch (DataExistsException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            mapper.writeValue(resp.getWriter(), Map.of("error", e.getMessage()));
        } catch (ValidationException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(resp.getWriter(), Map.of("error", e.getMessage()));
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
        setJsonHeaders(resp);
        String idParamStr = req.getParameter("id");
        if (idParamStr == null || idParamStr.isBlank()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(resp.getWriter(), Map.of("error", "Missing id parameter"));
            return;
        }

        int idParam;
        try {
            idParam = Integer.parseInt(idParamStr);
            if (idParam < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(resp.getWriter(), Map.of("error", "Invalid id parameter"));
            return;
        }

        TaskRequest updateDate;
        try {
            updateDate = mapper.readValue(req.getInputStream(), TaskRequest.class);
        } catch (JsonProcessingException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(resp.getWriter(), Map.of(
                    "error", "Invalid JSON format",
                    "details", e.getOriginalMessage()
            ));
            return;
        }

        try {
            validateData(updateDate);
            Integer userId = (Integer) req.getSession().getAttribute("id");
            Task updateData = service.update(idParam, updateDate, userId);
            TaskResponse taskResponse = new TaskResponse(updateData);
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), taskResponse);
        } catch (DataExistsException | InvalidStatusTransitionException e) {
            resp.setStatus(HttpServletResponse.SC_CONFLICT);
            mapper.writeValue(resp.getWriter(), Map.of("error", e.getMessage()));
        } catch (ValidationException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(resp.getWriter(), Map.of("error", e.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int idParam = Integer.parseInt(req.getParameter("id"));
        if (idParam < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter" + idParam);
            return;
        }
        try {
            service.deleteTask(idParam);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (DataExistsException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            mapper.writeValue(resp.getWriter(), Map.of("error", e.getMessage()));
        }
    }

    private void validateData(TaskRequest task) throws JsonProcessingException {
        Set<ConstraintViolation<TaskRequest>> violations = val.validate(task);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<TaskRequest> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("errors", errors);
            throw new ValidationException(mapper.writeValueAsString(responseBody));
        }
    }

    private void setJsonHeaders(HttpServletResponse resp) {
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(ENCODING_UTF8);
    }
}
