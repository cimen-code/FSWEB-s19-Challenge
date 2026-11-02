package com.workintech.s19challenge.controller;

import com.workintech.s19challenge.dto.RetweetRequest;
import com.workintech.s19challenge.dto.RetweetResponse;
import com.workintech.s19challenge.service.RetweetService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/retweet")
public class RetweetController {

    private final RetweetService retweetService;

    public RetweetController(RetweetService retweetService) {
        this.retweetService = retweetService;
    }

    @PostMapping({"", "/"})
    @ResponseStatus(HttpStatus.CREATED)
    public RetweetResponse create(
            @Valid @RequestBody RetweetRequest request,
            Authentication authentication) {

        return retweetService.create(
            authentication.getName(),
            request.tweetId()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            Authentication authentication) {

        retweetService.delete(
            id,
            authentication.getName()
        );
    }
}
