package com.workintech.s19challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workintech.s19challenge.entity.Retweet;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.RetweetRepository;
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
class RetweetFlowIT {

    private static final String PASSWORD =
        "hard-retweet-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TweetRepository tweetRepository;

    @Autowired
    private RetweetRepository retweetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createUser() {

        User user = new User();

        user.setUsername(
            "retweet_" +
            UUID.randomUUID().toString().replace("-", "")
        );

        user.setPasswordHash(
            passwordEncoder.encode(PASSWORD)
        );

        return userRepository.saveAndFlush(user);
    }

    private Tweet createTweet(User owner) {

        Tweet tweet = new Tweet();
        tweet.setUser(owner);
        tweet.setContent("Original tweet for retweet");

        return tweetRepository.saveAndFlush(tweet);
    }

    private ResultActions createRetweet(
            User user,
            Map<String, ?> payload) throws Exception {

        return mockMvc.perform(
            post("/retweet/")
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

    private ResultActions deleteRetweet(
            User user,
            Long id) throws Exception {

        return mockMvc.perform(
            delete("/retweet/{id}", id)
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        );
    }

    private Map<String, Long> body(Long tweetId) {
        return Map.of("tweetId", tweetId);
    }

    private Long createAndGetId(
            User user,
            Tweet tweet) throws Exception {

        String response = createRetweet(
            user,
            body(tweet.getId())
        )
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

        return objectMapper.readTree(response)
            .get("id")
            .asLong();
    }

    @Test
    void retweetUsesAuthenticatedUser()
            throws Exception {

        User tweetOwner = createUser();
        User reposter = createUser();
        User anotherUser = createUser();

        Tweet tweet = createTweet(tweetOwner);

        String response = createRetweet(
            reposter,
            Map.of(
                "tweetId", tweet.getId(),
                "userId", anotherUser.getId()
            )
        )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.tweetId").value(tweet.getId())
            )
            .andExpect(
                jsonPath("$.userId").value(reposter.getId())
            )
            .andExpect(jsonPath("$.id").isNumber())
            .andReturn()
            .getResponse()
            .getContentAsString();

        Long retweetId = objectMapper
            .readTree(response)
            .get("id")
            .asLong();

        Retweet saved = retweetRepository
            .findById(retweetId)
            .orElseThrow();

        assertEquals(
            reposter.getId(),
            saved.getUser().getId()
        );

        assertEquals(
            tweet.getId(),
            saved.getTweet().getId()
        );
    }

    @Test
    void twoUsersCanRetweetSameTweet()
            throws Exception {

        User owner = createUser();
        User first = createUser();
        User second = createUser();

        Tweet tweet = createTweet(owner);

        createAndGetId(first, tweet);
        createAndGetId(second, tweet);

        assertEquals(
            2L,
            retweetRepository.countByTweet_Id(tweet.getId())
        );
    }

    @Test
    void duplicateRetweetReturns409()
            throws Exception {

        User user = createUser();
        Tweet tweet = createTweet(user);

        createAndGetId(user, tweet);

        createRetweet(user, body(tweet.getId()))
            .andExpect(status().isConflict());

        assertEquals(
            1L,
            retweetRepository.countByTweet_Id(tweet.getId())
        );
    }

    @Test
    void ownerCanDeleteRetweet()
            throws Exception {

        User owner = createUser();
        User reposter = createUser();
        Tweet tweet = createTweet(owner);

        Long id = createAndGetId(reposter, tweet);

        deleteRetweet(reposter, id)
            .andExpect(status().isNoContent());

        assertFalse(
            retweetRepository.existsById(id)
        );
    }

    @Test
    void otherUserCannotDeleteRetweet()
            throws Exception {

        User owner = createUser();
        User reposter = createUser();
        User other = createUser();

        Tweet tweet = createTweet(owner);

        Long id = createAndGetId(reposter, tweet);

        deleteRetweet(other, id)
            .andExpect(status().isForbidden());

        assertTrue(
            retweetRepository.existsById(id)
        );
    }

    @Test
    void unknownRetweetReturns404()
            throws Exception {

        User user = createUser();

        deleteRetweet(
            user,
            9000000000000000L
        ).andExpect(status().isNotFound());
    }

    @Test
    void invalidRetweetDeleteIdReturns400()
            throws Exception {

        User user = createUser();

        deleteRetweet(user, 0L)
            .andExpect(status().isBadRequest());

        deleteRetweet(user, -1L)
            .andExpect(status().isBadRequest());
    }

    @Test
    void unknownTweetCannotBeRetweeted()
            throws Exception {

        User user = createUser();

        createRetweet(
            user,
            body(9000000000000000L)
        ).andExpect(status().isNotFound());
    }

    @Test
    void invalidRetweetBodyReturns400()
            throws Exception {

        User user = createUser();

        createRetweet(user, body(0L))
            .andExpect(status().isBadRequest());

        createRetweet(user, body(-1L))
            .andExpect(status().isBadRequest());

        createRetweet(user, Map.of())
            .andExpect(status().isBadRequest());
    }

    @Test
    void anonymousUserCannotRetweetOrDelete()
            throws Exception {

        String payload = objectMapper
            .writeValueAsString(body(1L));

        mockMvc.perform(
            post("/retweet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        ).andExpect(status().isUnauthorized());

        mockMvc.perform(
            delete("/retweet/1")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void deletingOriginalTweetRemovesRetweets()
            throws Exception {

        User owner = createUser();
        User reposter = createUser();

        Tweet tweet = createTweet(owner);

        Long retweetId =
            createAndGetId(reposter, tweet);

        assertTrue(
            retweetRepository.existsById(retweetId)
        );

        mockMvc.perform(
            delete("/tweet/{id}", tweet.getId())
                .with(httpBasic(
                    owner.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNoContent());

        assertEquals(
            0L,
            retweetRepository.countByTweet_Id(tweet.getId())
        );

        assertFalse(
            retweetRepository.existsById(retweetId)
        );
    }
}
