package org.example.controller;

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
import jakarta.validation.Validator;
import org.example.DTO.TaskDTO;
import org.example.service.TaskService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {

    private TaskService task;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        try {
            HikariDataSource ds = (HikariDataSource) getServletContext().getAttribute("datasource");
            Validator val = (Validator) getServletContext().getAttribute("validator");
            task = new TaskService(ds, val);
        } catch (Exception e) {
            throw new ServletException("Failed to initialize the library", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ArrayList<TaskDTO> data = task.takeAllElements();
        if (data.size() - 1 < 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Database is empty");
        }
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getWriter(), data);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TaskDTO createTask = mapper.readValue(req.getInputStream(), TaskDTO.class);
        Optional<TaskDTO> create = task.createTask(createTask);
        if (create.isPresent()) {
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            mapper.writeValue(resp.getWriter(), create.get());
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "This task is already created");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TaskDTO up = mapper.readValue(req.getInputStream(), TaskDTO.class);
        int id = Integer.parseInt(req.getParameter("id"));
        Optional<TaskDTO> updateData = task.update(id, up);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        if (updateData.isPresent()) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            mapper.writeValue(resp.getWriter(), updateData);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("{\"error\": \"Invalid parameters\"}");
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
            resp.getWriter().write("The data of id" + idParam + " is deleted successfully");
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Task is not found");
        }
    }
}
