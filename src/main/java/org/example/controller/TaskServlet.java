package org.example.controller;


import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.DTO.TaskDTO;
import org.example.service.TaskService;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {

    private TaskService task;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            task = new TaskService();
        } catch (Exception e) {
            throw new ServletException("Не удалось инициализировать библиотеку", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ArrayList<TaskDTO> data = task.takeAllElements();
        PrintWriter test = resp.getWriter();
        for (TaskDTO allData : data) {
            test.println("<html>");
            test.println("<h1>" + allData.getTitle() + " " + allData.getDescription() + " " + allData.getStatus() + " " + allData.getDate() + "</h1>");
            test.println("</html>");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String s = req.getParameter("title");
        task.createTask();
        PrintWriter test = resp.getWriter();
        test.println("<html>");
        test.println("<h1>" + s + "</h1>");
        test.println("<h1>" + "insert success" + "</h1>");
        test.println("</html>");
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        task.update();
        PrintWriter test = resp.getWriter();
        test.println("<html>");
        test.println("<h1>" + "update success" + "</h1>");
        test.println("</html>");
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
