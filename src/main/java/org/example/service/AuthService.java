package org.example.service;

import com.zaxxer.hikari.HikariDataSource;
import org.example.dto.LoginRequest;
import org.example.model.Login;
import org.example.repository.AuthRepo;

public class AuthService {

    private final AuthRepo repo;

    public AuthService(HikariDataSource hikariDataSource) {
        this.repo = new AuthRepo(hikariDataSource);
    }

    public Login isRegister(LoginRequest loginRequest) {
        return repo.verify(loginRequest.getUsername(), loginRequest.getPassword());
    }
}
