package com.workintech.s19challenge.controller;

import com.workintech.s19challenge.dto.AuthRequest;
import com.workintech.s19challenge.dto.AuthResponse;
import com.workintech.s19challenge.service.AuthenticationService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(
            @Valid @RequestBody AuthRequest request) {

        return authenticationService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody AuthRequest request) {

        return authenticationService.login(request);
    }
}
