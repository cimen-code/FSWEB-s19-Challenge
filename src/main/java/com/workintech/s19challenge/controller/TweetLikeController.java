package com.workintech.s19challenge.controller;

import com.workintech.s19challenge.dto.TweetLikeRequest;
import com.workintech.s19challenge.dto.TweetLikeResponse;
import com.workintech.s19challenge.service.TweetLikeService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class TweetLikeController {

    private final TweetLikeService tweetLikeService;

    public TweetLikeController(TweetLikeService tweetLikeService) {
        this.tweetLikeService = tweetLikeService;
    }

    @PostMapping({"/like", "/like/"})
    @ResponseStatus(HttpStatus.CREATED)
    public TweetLikeResponse like(
            @Valid @RequestBody TweetLikeRequest request,
            Authentication authentication) {

        return tweetLikeService.like(
            authentication.getName(),
            request.tweetId()
        );
    }

    @PostMapping({"/dislike", "/dislike/"})
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void dislike(
            @Valid @RequestBody TweetLikeRequest request,
            Authentication authentication) {

        tweetLikeService.dislike(
            authentication.getName(),
            request.tweetId()
        );
    }
}
