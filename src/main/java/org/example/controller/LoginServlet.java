package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.exception.ErrorResponse;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.service.TaskService;


public class LoginServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();
    private TaskService service;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            Object dsObj = getServletContext().getAttribute("datasource");
            HikariDataSource ds = (HikariDataSource) dsObj;
            service = new TaskService(ds);
        } catch (Exception e) {
            throw new ServletException("Failed to initialize the library", e);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        LoginRequest login;
        try {
            login = mapper.readValue(req.getInputStream(), LoginRequest.class);
        } catch (JsonProcessingException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(resp.getWriter(), new ErrorResponse("Malformed or missing JSON login payload"));
            return;
        }
        boolean register;
        try {
            register = service.isRegister(login);
        }catch (RuntimeException e){
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            mapper.writeValue(resp.getWriter(), e.getMessage());
            return;
        }


        if (!register) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(resp.getWriter(), new ErrorResponse("Invalid credentials"));
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("user", login.username);

        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setSecure(false);
        cookie.setAttribute("SameSite", "Lax");
        resp.addCookie(cookie);

        resp.setStatus(HttpServletResponse.SC_OK);
        mapper.writeValue(resp.getWriter(), new LoginResponse("ok"));
    }
}
