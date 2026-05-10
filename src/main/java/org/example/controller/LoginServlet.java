package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.exception.ErrorResponse;

import java.io.IOException;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        LoginRequest login = mapper.readValue(req.getInputStream(), LoginRequest.class);

        if (!login.username.equals("admin") || !login.password.equals("123")) {
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

        resp.setContentType("application/json");
        mapper.writeValue(resp.getWriter(), new LoginResponse("ok"));
    }
}
