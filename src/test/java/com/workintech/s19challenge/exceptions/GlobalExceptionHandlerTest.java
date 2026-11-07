package com.workintech.s19challenge.exceptions;

import org.junit.jupiter.api.Test;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
        new GlobalExceptionHandler();

    @Test
    void lostDeleteRaceReturns404() {

        ResponseEntity<Map<String, Object>> response =
            handler.handleAlreadyDeleted(
                new OptimisticLockingFailureException("row deleted")
            );

        assertEquals(404, response.getStatusCode().value());
    }

    @Test
    void responseStatusKeepsStatusAndReason() {

        ResponseEntity<Map<String, Object>> response =
            handler.handleStatus(
                new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only the tweet owner can change this tweet"
                )
            );

        assertEquals(403, response.getStatusCode().value());

        assertEquals(
            "Only the tweet owner can change this tweet",
            response.getBody().get("message")
        );
    }

    @Test
    void unsupportedMethodIsMethodNotAllowed() {

        ResponseEntity<Map<String, Object>> response =
            handler.handleRequestMapping(
                new HttpRequestMethodNotSupportedException(
                    "GET",
                    List.of("PUT", "DELETE")
                )
            );

        assertEquals(405, response.getStatusCode().value());
        assertEquals("Method Not Allowed", response.getBody().get("message"));

        assertEquals(
            Set.of(HttpMethod.PUT, HttpMethod.DELETE),
            response.getHeaders().getAllow()
        );
    }

    @Test
    void unknownPathIsNotFound() {

        ResponseEntity<Map<String, Object>> response =
            handler.handleRequestMapping(
                new NoResourceFoundException(HttpMethod.GET, "unknown")
            );

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Not Found", response.getBody().get("message"));
    }

    @Test
    void dataIntegrityViolationIsConflict() {

        ResponseEntity<Map<String, Object>> response =
            handler.handleConflict(
                new DataIntegrityViolationException("duplicate key")
            );

        assertEquals(409, response.getStatusCode().value());
    }

    @Test
    void unexpectedErrorHidesDetails() {

        ResponseEntity<Map<String, Object>> response =
            handler.handleUnexpected(
                new IllegalStateException("database password")
            );

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal server error", response.getBody().get("message"));
    }
}
