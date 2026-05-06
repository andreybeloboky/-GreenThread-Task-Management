package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;
import org.example.model.Subtask;
import org.example.model.Task;
import org.example.service.TaskService;

import java.io.IOException;
import java.util.ArrayList;

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

    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    }

    private void setJsonHeaders(HttpServletResponse resp) {
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(ENCODING_UTF8);
    }
}
