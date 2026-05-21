package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.exception.DataNotExistsException;
import org.example.exception.ErrorResponse;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.example.model.Login;
import org.example.service.AuthService;

import static org.example.controller.TaskServlet.CONTENT_TYPE_JSON;
import static org.example.controller.TaskServlet.ENCODING_UTF8;


public class LoginServlet extends HttpServlet {

    private ObjectMapper mapper;
    private AuthService authService;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            mapper = (ObjectMapper) getServletContext().getAttribute("mapper");
            authService = (AuthService) getServletContext().getAttribute("auth");
        } catch (Exception e) {
            throw new ServletException("Failed to initialize the library", e);
        }

    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        setJsonHeaders(resp);

        LoginRequest login;
        try {
            login = mapper.readValue(req.getInputStream(), LoginRequest.class);
        } catch (JsonProcessingException e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            mapper.writeValue(resp.getWriter(), new ErrorResponse("Malformed or missing JSON login payload"));
            return;
        }
        Login register;
        try {
            register = authService.isRegister(login);
        } catch (DataNotExistsException e) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            mapper.writeValue(resp.getWriter(), e.getMessage());
            return;
        }

        HttpSession session = req.getSession(true);
        session.setAttribute("user", register.getLogin());
        session.setAttribute("id", register.getId());

        resp.setStatus(HttpServletResponse.SC_OK);
        mapper.writeValue(resp.getWriter(), new LoginResponse("ok"));
    }

    private void setJsonHeaders(HttpServletResponse resp) {
        resp.setContentType(CONTENT_TYPE_JSON);
        resp.setCharacterEncoding(ENCODING_UTF8);
    }
}
