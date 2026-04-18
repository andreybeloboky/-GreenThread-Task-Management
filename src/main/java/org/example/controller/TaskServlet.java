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
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {

    private TaskService task;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
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
        if (data.size() - 1 == 0) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Database is empty");
        }
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        mapper.writeValue(resp.getWriter(), data);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        TaskDTO createTask = mapper.readValue(req.getInputStream(), TaskDTO.class);
        TaskDTO create = task.createTask(createTask);
        if (create != null) {
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            mapper.writeValue(resp.getWriter(), create);
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameters aren't correct");
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        TaskDTO up = mapper.readValue(req.getInputStream(), TaskDTO.class);


        int id = Integer.parseInt(req.getParameter("id"));
        boolean updateData = task.update(id, up);
        if (updateData) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");

            // ???
        } else {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);

            // зачем setContent ?? можно же без него
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\": \"Invalid parameters\"}");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String idParam = req.getParameter("id");
        if (idParam == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing id parameter");
            return;
        }
        int id = Integer.parseInt(idParam);
        boolean deleted = task.delete(id);
        if (deleted) {
            resp.setStatus(HttpServletResponse.SC_NO_CONTENT);
        } else {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Task not found");
        }
    }
}
