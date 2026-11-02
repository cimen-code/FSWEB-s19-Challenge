package com.workintech.s19challenge.repository;

import com.workintech.s19challenge.entity.Tweet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TweetRepository
        extends JpaRepository<Tweet, Long> {

    List<Tweet> findByUser_Id(Long userId);
}
