package com.workintech.s19challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthenticationFlowIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final String PASSWORD = "test-only-password";

    private String newUsername() {
        return "challenge_auth_" +
            UUID.randomUUID().toString().replace("-", "");
    }

    private String json(String username, String password)
            throws Exception {

        return objectMapper.writeValueAsString(
            Map.of(
                "username", username,
                "password", password
            )
        );
    }

    private void register(String username)
            throws Exception {

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(username, PASSWORD))
        ).andExpect(status().isCreated());
    }

    @Test
    void registerSavesUserWithBCryptPassword() throws Exception {

        String username = newUsername();

        register(username);

        User saved = userRepository
            .findByUsername(username)
            .orElseThrow();

        assertNotNull(saved.getId());

        assertNotEquals(
            PASSWORD,
            saved.getPasswordHash()
        );

        assertTrue(
            passwordEncoder.matches(
                PASSWORD,
                saved.getPasswordHash()
            )
        );
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {

        String username = newUsername();

        register(username);

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(username, PASSWORD))
        ).andExpect(status().isConflict());
    }

    @Test
    void blankUsernameReturns400() throws Exception {

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(" ", PASSWORD))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void loginValidatesPassword() throws Exception {

        String username = newUsername();

        register(username);

        mockMvc.perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(username, PASSWORD))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value(username));

        mockMvc.perform(
            post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(username, "wrong-password"))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousTweetAccessIsRejected() throws Exception {

        mockMvc.perform(
            get("/tweet/findByUserId")
        ).andExpect(status().isUnauthorized());

        mockMvc.perform(
            post("/tweet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"test\"}")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void colonInUsernameReturns400() throws Exception {

        mockMvc.perform(
                post("/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json("a:b", PASSWORD))
            )
            .andExpect(status().isBadRequest());
    }

    @Test
    void passwordLengthIsValidated() throws Exception {

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(newUsername(), "short"))
        ).andExpect(status().isBadRequest());

        mockMvc.perform(
            post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(newUsername(), "x".repeat(73)))
        ).andExpect(status().isBadRequest());
    }
}
