package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.AuthRequest;
import com.workintech.s19challenge.dto.AuthResponse;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.UserRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    public AuthenticationService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public AuthResponse register(AuthRequest request) {

        String username = request.username().trim();

        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Username already exists"
            );
        }

        User user = new User();

        user.setUsername(username);
        user.setPasswordHash(
            passwordEncoder.encode(request.password())
        );

        try {
            User saved = userRepository.saveAndFlush(user);

            return new AuthResponse(
                saved.getId(),
                saved.getUsername(),
                "Registration successful"
            );

        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Username already exists"
            );
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {

        String username = request.username().trim();

        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    username,
                    request.password()
                )
            );

        } catch (AuthenticationException exception) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid username or password"
            );
        }

        User user = userRepository.findByUsername(username)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid username or password"
                )
            );

        return new AuthResponse(
            user.getId(),
            user.getUsername(),
            "Login successful"
        );
    }
}
