package com.workintech.s19challenge.repository;

import com.workintech.s19challenge.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository
        extends JpaRepository<Comment, Long> {

    List<Comment> findByTweet_IdOrderByIdAsc(Long tweetId);
}
