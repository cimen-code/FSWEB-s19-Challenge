package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.RetweetResponse;

public interface RetweetService {

    RetweetResponse create(
        String username,
        Long tweetId
    );

    void delete(
        Long id,
        String username
    );
}
