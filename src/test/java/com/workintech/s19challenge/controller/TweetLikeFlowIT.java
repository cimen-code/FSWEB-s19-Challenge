package com.workintech.s19challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.TweetLikeRepository;
import com.workintech.s19challenge.repository.TweetRepository;
import com.workintech.s19challenge.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TweetLikeFlowIT {

    private static final String PASSWORD =
        "medium-like-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TweetRepository tweetRepository;

    @Autowired
    private TweetLikeRepository tweetLikeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createUser() {

        User user = new User();

        user.setUsername(
            "like_" +
            UUID.randomUUID().toString().replace("-", "")
        );

        user.setPasswordHash(
            passwordEncoder.encode(PASSWORD)
        );

        return userRepository.saveAndFlush(user);
    }

    private Tweet createTweet(User user) {

        Tweet tweet = new Tweet();

        tweet.setUser(user);
        tweet.setContent("Tweet for like tests");

        return tweetRepository.saveAndFlush(tweet);
    }

    private Map<String, Long> body(Long tweetId) {
        return Map.of("tweetId", tweetId);
    }

    private ResultActions send(
            String endpoint,
            User user,
            Object payload) throws Exception {

        return mockMvc.perform(
            post(endpoint)
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(payload)
                )
        );
    }

    private boolean hasLike(User user, Tweet tweet) {

        return tweetLikeRepository
            .existsByUser_IdAndTweet_Id(
                user.getId(),
                tweet.getId()
            );
    }

    @Test
    void authenticatedUserCanLikeTweet()
            throws Exception {

        User owner = createUser();
        User liker = createUser();
        Tweet tweet = createTweet(owner);

        send("/like", liker, body(tweet.getId()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNumber())
            .andExpect(
                jsonPath("$.userId").value(liker.getId())
            )
            .andExpect(
                jsonPath("$.tweetId").value(tweet.getId())
            );

        assertTrue(hasLike(liker, tweet));
    }

    @Test
    void twoDifferentUsersCanLikeSameTweet()
            throws Exception {

        User owner = createUser();
        User second = createUser();

        Tweet tweet = createTweet(owner);

        send("/like", owner, body(tweet.getId()))
            .andExpect(status().isCreated());

        send("/like", second, body(tweet.getId()))
            .andExpect(status().isCreated());

        assertEquals(
            2L,
            tweetLikeRepository.countByTweet_Id(tweet.getId())
        );
    }

    @Test
    void sameUserCannotLikeTwice()
            throws Exception {

        User user = createUser();
        Tweet tweet = createTweet(user);

        send("/like", user, body(tweet.getId()))
            .andExpect(status().isCreated());

        send("/like", user, body(tweet.getId()))
            .andExpect(status().isConflict());

        assertEquals(
            1L,
            tweetLikeRepository.countByTweet_Id(tweet.getId())
        );
    }

    @Test
    void userCanRemoveTheirOwnLike()
            throws Exception {

        User user = createUser();
        Tweet tweet = createTweet(user);

        send("/like", user, body(tweet.getId()))
            .andExpect(status().isCreated());

        assertTrue(hasLike(user, tweet));

        send("/dislike", user, body(tweet.getId()))
            .andExpect(status().isNoContent());

        assertFalse(hasLike(user, tweet));
    }

    @Test
    void userCannotRemoveAnotherUsersLike()
            throws Exception {

        User owner = createUser();
        User other = createUser();
        Tweet tweet = createTweet(owner);

        send("/like", owner, body(tweet.getId()))
            .andExpect(status().isCreated());

        send("/dislike", other, body(tweet.getId()))
            .andExpect(status().isNotFound());

        assertTrue(hasLike(owner, tweet));
    }

    @Test
    void dislikeWithoutExistingLikeReturns404()
            throws Exception {

        User user = createUser();
        Tweet tweet = createTweet(user);

        send("/dislike", user, body(tweet.getId()))
            .andExpect(status().isNotFound());
    }

    @Test
    void unknownTweetCannotBeLiked()
            throws Exception {

        User user = createUser();

        send(
            "/like",
            user,
            body(9000000000000000L)
        ).andExpect(status().isNotFound());
    }

    @Test
    void invalidLikePayloadReturns400()
            throws Exception {

        User user = createUser();

        send("/like", user, body(0L))
            .andExpect(status().isBadRequest());

        send("/like", user, body(-1L))
            .andExpect(status().isBadRequest());

        send("/like", user, Map.of())
            .andExpect(status().isBadRequest());

        send("/dislike", user, body(0L))
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousUserCannotLikeOrDislike()
            throws Exception {

        User user = createUser();
        Tweet tweet = createTweet(user);

        String payload = objectMapper
            .writeValueAsString(body(tweet.getId()));

        mockMvc.perform(
            post("/like")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        ).andExpect(status().isUnauthorized());

        mockMvc.perform(
            post("/dislike")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void requestCannotImpersonateAnotherUser()
            throws Exception {

        User first = createUser();
        User second = createUser();

        Tweet tweet = createTweet(first);

        send(
            "/like",
            first,
            Map.of(
                "tweetId", tweet.getId(),
                "userId", second.getId()
            )
        )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.userId").value(first.getId())
            );

        assertTrue(hasLike(first, tweet));
        assertFalse(hasLike(second, tweet));
    }

    @Test
    void userCanLikeAgainAfterDislike()
            throws Exception {

        User user = createUser();
        Tweet tweet = createTweet(user);

        send("/like", user, body(tweet.getId()))
            .andExpect(status().isCreated());

        send("/dislike", user, body(tweet.getId()))
            .andExpect(status().isNoContent());

        send("/like", user, body(tweet.getId()))
            .andExpect(status().isCreated());

        assertEquals(
            1L,
            tweetLikeRepository.countByTweet_Id(tweet.getId())
        );
    }
}
