package org.example.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.example.exception.DataExistsException;
import org.example.exception.DataNotExistsException;
import org.example.exception.InvalidStatusTransitionException;
import org.example.model.Subtask;
import org.example.service.TaskService;

import java.io.IOException;
import java.util.*;

@WebServlet("/subtasks")
public class SubtaskServlet extends HttpServlet {

    private TaskService service;
    private ObjectMapper mapper;
    private Validator val;
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String ENCODING_UTF8 = "UTF-8";

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            val = (Validator) getServletContext().getAttribute("validator");
            service = (TaskService) getServletContext().getAttribute("service");
            mapper = (ObjectMapper) getServletContext().getAttribute("mapper");
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        } catch (Exception e) {
            throw new ServletException("Failed to initialize the library", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ArrayList<Subtask> data = service.findAllSubtask();
        setJsonHeaders(resp);
        if (data == null || data.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), new ArrayList<>());
            return;
        }
        List<SubtaskResponse> sbResponse = data.stream().map(SubtaskResponse::new).toList();
        resp.setStatus(HttpServletResponse.SC_OK);
        mapper.writeValue(resp.getWriter(), sbResponse);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            SubtaskRequest subtaskReq = mapper.readValue(req.getInputStream(), SubtaskRequest.class);
            validateData(subtaskReq);
            Subtask subtask = service.createSubtask(subtaskReq);
            SubtaskResponse subtaskResponse = new SubtaskResponse(subtask);

            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            String appName = req.getContextPath();
            resp.setHeader("Location", appName + "/subtasks/" + subtaskResponse.getId());
            mapper.writeValue(resp.getWriter(), subtaskResponse);
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
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
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

        SubtaskRequest updateDate;
        try {
            updateDate = mapper.readValue(req.getInputStream(), SubtaskRequest.class);
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
            Subtask subtask = service.updateSubtask(idParam, updateDate);
            SubtaskResponse taskDTO = new SubtaskResponse(subtask);
            resp.setStatus(HttpServletResponse.SC_OK);
            mapper.writeValue(resp.getWriter(), taskDTO);
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
            service.deleteSubtask(idParam);
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } catch (DataNotExistsException e) {
            setJsonHeaders(resp);
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            mapper.writeValue(resp.getWriter(), Map.of("error", e.getMessage()));
        }
    }

    private void setJsonHeaders(HttpServletResponse resp) {
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(ENCODING_UTF8);
    }

    private void validateData(SubtaskRequest subtaskRequest) throws JsonProcessingException {
        Set<ConstraintViolation<SubtaskRequest>> violations = val.validate(subtaskRequest);
        if (!violations.isEmpty()) {
            Map<String, String> errors = new HashMap<>();
            for (ConstraintViolation<SubtaskRequest> violation : violations) {
                errors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            Map<String, Object> responseBody = new HashMap<>();
            responseBody.put("errors", errors);
            throw new ValidationException(mapper.writeValueAsString(responseBody));
        }
    }
}
