package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.CommentRequest;
import com.workintech.s19challenge.dto.CommentResponse;
import com.workintech.s19challenge.dto.CommentUpdateRequest;

public interface CommentService {

    CommentResponse create(
        String username,
        CommentRequest request
    );

    CommentResponse update(
        Long id,
        String username,
        CommentUpdateRequest request
    );

    void delete(
        Long id,
        String username
    );
}
