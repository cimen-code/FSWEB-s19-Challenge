package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.RetweetResponse;
import com.workintech.s19challenge.entity.Retweet;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.RetweetRepository;
import com.workintech.s19challenge.repository.TweetRepository;
import com.workintech.s19challenge.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class RetweetServiceImpl implements RetweetService {

    private final RetweetRepository retweetRepository;
    private final TweetRepository tweetRepository;
    private final UserRepository userRepository;

    public RetweetServiceImpl(
            RetweetRepository retweetRepository,
            TweetRepository tweetRepository,
            UserRepository userRepository) {

        this.retweetRepository = retweetRepository;
        this.tweetRepository = tweetRepository;
        this.userRepository = userRepository;
    }

    @Override
    public RetweetResponse create(
            String username,
            Long tweetId) {

        User user = userRepository
            .findByUsername(username)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
                )
            );

        Tweet tweet = tweetRepository
            .findById(tweetId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tweet not found"
                )
            );

        if (retweetRepository.existsByUser_IdAndTweet_Id(
                user.getId(),
                tweetId)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Tweet already retweeted"
            );
        }

        Retweet retweet = new Retweet();

        retweet.setUser(user);
        retweet.setTweet(tweet);

        Retweet saved =
            retweetRepository.saveAndFlush(retweet);

        return new RetweetResponse(
            saved.getId(),
            saved.getTweet().getId(),
            saved.getUser().getId()
        );
    }

    @Override
    public void delete(Long id, String username) {

        if (id <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "ID must be positive"
            );
        }

        Retweet retweet = retweetRepository
            .findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Retweet not found"
                )
            );

        if (!retweet.getUser()
                .getUsername()
                .equals(username)) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the retweet owner can delete it"
            );
        }

        retweetRepository.delete(retweet);
        retweetRepository.flush();
    }
}
