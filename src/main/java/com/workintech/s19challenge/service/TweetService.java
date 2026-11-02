package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.TweetRequest;
import com.workintech.s19challenge.dto.TweetResponse;

import java.util.List;

public interface TweetService {

    TweetResponse create(
        String username,
        TweetRequest request
    );

    List<TweetResponse> findByUserId(Long userId);

    TweetResponse findById(Long id);

    TweetResponse update(
        Long id,
        String username,
        TweetRequest request
    );

    void delete(Long id, String username);
}
