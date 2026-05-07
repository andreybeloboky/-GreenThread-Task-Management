package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.example.dto.SubtaskRequest;
import org.example.dto.SubtaskResponse;
import org.example.dto.TaskRequest;
import org.example.exception.DataExistsException;
import org.example.model.Subtask;
import org.example.model.Task;
import org.example.service.TaskService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@WebServlet("/subtasks")
public class SubtaskServlet extends HttpServlet {

    private TaskService task;
    private final ObjectMapper mapper = new ObjectMapper();
    private Validator val;
    private HikariDataSource ds;
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String ENCODING_UTF8 = "UTF-8";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        ArrayList<Subtask> data = task.findAllSubtask();
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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            SubtaskRequest subtaskReq = mapper.readValue(req.getInputStream(), SubtaskRequest.class);
            validateData(subtaskReq);
            Subtask subtask = task.createSubtask(subtaskReq);
            SubtaskResponse subtaskResponse = new SubtaskResponse(subtask);

            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            String appName = req.getContextPath();
            resp.setHeader("Location", appName + "/subtasks/" + subtaskResponse.getId());
            mapper.writeValue(resp.getWriter(), subtaskResponse);
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
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        int idParam = Integer.parseInt(req.getParameter("id"));
        if (idParam < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter" + idParam);
            return;
        }
        try {
            task.deleteSubtask(idParam);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (DataExistsException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            mapper.writeValue(resp.getWriter(), Map.of("error", e.getMessage()));
        }
    }

    private void setJsonHeaders(HttpServletResponse resp) {
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(ENCODING_UTF8);
    }

    private void validateData(SubtaskRequest task) throws JsonProcessingException {
        Set<ConstraintViolation<SubtaskRequest>> violations = val.validate(task);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<SubtaskRequest> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("errors", errors);
            responseBody.put("test", "test"); // todo test!!!!
            throw new ValidationException(mapper.writeValueAsString(responseBody));
        }
    }
}
