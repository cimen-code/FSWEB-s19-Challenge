package com.workintech.s19challenge.controller;

import com.workintech.s19challenge.dto.TweetRequest;
import com.workintech.s19challenge.dto.TweetResponse;
import com.workintech.s19challenge.service.TweetService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tweet")
public class TweetController {

    private final TweetService tweetService;

    public TweetController(TweetService tweetService) {
        this.tweetService = tweetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TweetResponse create(
            @Valid @RequestBody TweetRequest request,
            Authentication authentication) {

        return tweetService.create(
            authentication.getName(),
            request
        );
    }

    @GetMapping("/findByUserId")
    public List<TweetResponse> findByUserId(
            @RequestParam Long userId) {

        return tweetService.findByUserId(userId);
    }

    @GetMapping("/findById")
    public TweetResponse findById(
            @RequestParam Long id) {

        return tweetService.findById(id);
    }

    @PutMapping("/{id}")
    public TweetResponse update(
            @PathVariable Long id,
            @Valid @RequestBody TweetRequest request,
            Authentication authentication) {

        return tweetService.update(
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

        tweetService.delete(
            id,
            authentication.getName()
        );
    }
}
