package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.TweetLikeResponse;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.TweetLike;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.TweetLikeRepository;
import com.workintech.s19challenge.repository.TweetRepository;
import com.workintech.s19challenge.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class TweetLikeServiceImpl implements TweetLikeService {

    private final TweetLikeRepository tweetLikeRepository;
    private final TweetRepository tweetRepository;
    private final UserRepository userRepository;

    public TweetLikeServiceImpl(
            TweetLikeRepository tweetLikeRepository,
            TweetRepository tweetRepository,
            UserRepository userRepository) {

        this.tweetLikeRepository = tweetLikeRepository;
        this.tweetRepository = tweetRepository;
        this.userRepository = userRepository;
    }

    @Override
    public TweetLikeResponse like(
            String username,
            Long tweetId) {

        User user = findUser(username);

        Tweet tweet = tweetRepository
            .findById(tweetId)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tweet not found"
                )
            );

        if (tweetLikeRepository.existsByUser_IdAndTweet_Id(
                user.getId(),
                tweetId)) {

            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Tweet already liked"
            );
        }

        TweetLike tweetLike = new TweetLike();

        tweetLike.setUser(user);
        tweetLike.setTweet(tweet);

        TweetLike saved =
            tweetLikeRepository.saveAndFlush(tweetLike);

        return new TweetLikeResponse(
            saved.getId(),
            saved.getTweet().getId(),
            saved.getUser().getId()
        );
    }

    @Override
    public void dislike(
            String username,
            Long tweetId) {

        User user = findUser(username);

        TweetLike tweetLike = tweetLikeRepository
            .findByUser_IdAndTweet_Id(
                user.getId(),
                tweetId
            )
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Like not found"
                )
            );

        tweetLikeRepository.delete(tweetLike);
        tweetLikeRepository.flush();
    }

    private User findUser(String username) {

        return userRepository
            .findByUsername(username)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
                )
            );
    }
}
