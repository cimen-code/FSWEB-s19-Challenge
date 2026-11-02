package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.TweetRequest;
import com.workintech.s19challenge.dto.TweetResponse;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.TweetRepository;
import com.workintech.s19challenge.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional
public class TweetServiceImpl implements TweetService {

    private final TweetRepository tweetRepository;
    private final UserRepository userRepository;

    public TweetServiceImpl(
            TweetRepository tweetRepository,
            UserRepository userRepository) {

        this.tweetRepository = tweetRepository;
        this.userRepository = userRepository;
    }

    @Override
    public TweetResponse create(
            String username,
            TweetRequest request) {

        User user = userRepository
            .findByUsername(username)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
                )
            );

        Tweet tweet = new Tweet();
        tweet.setUser(user);
        tweet.setContent(request.content());

        return toResponse(
            tweetRepository.saveAndFlush(tweet)
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<TweetResponse> findByUserId(Long userId) {

        validateId(userId);

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "User not found"
            );
        }

        return tweetRepository
            .findByUser_Id(userId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TweetResponse findById(Long id) {

        return toResponse(findTweet(id));
    }

    @Override
    public TweetResponse update(
            Long id,
            String username,
            TweetRequest request) {

        Tweet tweet = findTweet(id);

        verifyOwner(tweet, username);

        tweet.setContent(request.content());

        return toResponse(
            tweetRepository.saveAndFlush(tweet)
        );
    }

    @Override
    public void delete(Long id, String username) {

        Tweet tweet = findTweet(id);

        verifyOwner(tweet, username);

        tweetRepository.delete(tweet);
        tweetRepository.flush();
    }

    private Tweet findTweet(Long id) {

        validateId(id);

        return tweetRepository
            .findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tweet not found"
                )
            );
    }

    private void verifyOwner(
            Tweet tweet,
            String username) {

        if (!tweet.getUser()
                .getUsername()
                .equals(username)) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the tweet owner can change this tweet"
            );
        }
    }

    private void validateId(Long id) {

        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "ID must be positive"
            );
        }
    }

    private TweetResponse toResponse(Tweet tweet) {

        return new TweetResponse(
            tweet.getId(),
            tweet.getUser().getId(),
            tweet.getUser().getUsername(),
            tweet.getContent()
        );
    }
}
