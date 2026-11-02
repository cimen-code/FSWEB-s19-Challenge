package com.workintech.s19challenge.controller;

import com.workintech.s19challenge.dto.CommentRequest;
import com.workintech.s19challenge.dto.CommentResponse;
import com.workintech.s19challenge.dto.CommentUpdateRequest;
import com.workintech.s19challenge.service.CommentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @Valid @RequestBody CommentRequest request,
            Authentication authentication) {

        return commentService.create(
            authentication.getName(),
            request
        );
    }

    @PutMapping("/{id}")
    public CommentResponse update(
            @PathVariable Long id,
            @Valid @RequestBody CommentUpdateRequest request,
            Authentication authentication) {

        return commentService.update(
            id,
            authentication.getName(),
            request
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {

        commentService.delete(
            id,
            authentication.getName()
        );
    }
}
