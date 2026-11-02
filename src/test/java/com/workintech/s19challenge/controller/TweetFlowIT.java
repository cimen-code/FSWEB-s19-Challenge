package com.workintech.s19challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.TweetRepository;
import com.workintech.s19challenge.repository.UserRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TweetFlowIT {

    private static final String PASSWORD =
        "easy-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TweetRepository tweetRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createUser() {

        User user = new User();

        user.setUsername(
            "easy_" +
            UUID.randomUUID().toString().replace("-", "")
        );

        user.setPasswordHash(
            passwordEncoder.encode(PASSWORD)
        );

        return userRepository.saveAndFlush(user);
    }

    private Tweet createTweet(
            User user,
            String content) {

        Tweet tweet = new Tweet();

        tweet.setUser(user);
        tweet.setContent(content);

        return tweetRepository.saveAndFlush(tweet);
    }

    private String json(String content) throws Exception {

        return objectMapper.writeValueAsString(
            Map.of("content", content)
        );
    }

    @Test
    void createsTweetForAuthenticatedUser() throws Exception {

        User owner = createUser();
        User differentUser = createUser();

        String payload = objectMapper.writeValueAsString(
            Map.of(
                "content", "My first tweet",
                "userId", differentUser.getId()
            )
        );

        var result = mockMvc.perform(
            post("/tweet")
                .with(httpBasic(
                    owner.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload)
        )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.username")
                    .value(owner.getUsername())
            )
            .andExpect(
                jsonPath("$.userId")
                    .value(owner.getId())
            )
            .andReturn();

        Long tweetId = objectMapper
            .readTree(result.getResponse().getContentAsString())
            .get("id")
            .asLong();

        Tweet saved = tweetRepository
            .findById(tweetId)
            .orElseThrow();

        assertEquals(
            owner.getId(),
            saved.getUser().getId()
        );
    }

    @Test
    void findsTweetsOnlyForRequestedUser() throws Exception {

        User first = createUser();
        User second = createUser();

        Tweet expected = createTweet(first, "First");
        createTweet(second, "Second");

        mockMvc.perform(
            get("/tweet/findByUserId")
                .param("userId", first.getId().toString())
                .with(httpBasic(
                    first.getUsername(),
                    PASSWORD
                ))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(
                jsonPath("$[0].id")
                    .value(expected.getId())
            );
    }

    @Test
    void findsTweetById() throws Exception {

        User user = createUser();

        Tweet tweet = createTweet(user, "Find me");

        mockMvc.perform(
            get("/tweet/findById")
                .param("id", tweet.getId().toString())
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(tweet.getId()))
            .andExpect(jsonPath("$.content").value("Find me"))
            .andExpect(
                jsonPath("$.username")
                    .value(user.getUsername())
            )
            .andExpect(
                jsonPath("$.passwordHash").doesNotExist()
            );
    }

    @Test
    void ownerCanUpdateTweet() throws Exception {

        User owner = createUser();

        Tweet tweet = createTweet(owner, "Before");

        mockMvc.perform(
            put("/tweet/{id}", tweet.getId())
                .with(httpBasic(
                    owner.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("After"))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").value("After"));

        assertEquals(
            "After",
            tweetRepository.findById(tweet.getId())
                .orElseThrow()
                .getContent()
        );
    }

    @Test
    void otherUserCannotUpdateTweet() throws Exception {

        User owner = createUser();
        User other = createUser();

        Tweet tweet = createTweet(owner, "Original");

        mockMvc.perform(
            put("/tweet/{id}", tweet.getId())
                .with(httpBasic(
                    other.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("Unauthorized"))
        ).andExpect(status().isForbidden());

        assertEquals(
            "Original",
            tweetRepository.findById(tweet.getId())
                .orElseThrow()
                .getContent()
        );
    }

    @Test
    void otherUserCannotDeleteTweet() throws Exception {

        User owner = createUser();
        User other = createUser();

        Tweet tweet = createTweet(owner, "Protected");

        mockMvc.perform(
            delete("/tweet/{id}", tweet.getId())
                .with(httpBasic(
                    other.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isForbidden());

        assertTrue(tweetRepository.existsById(tweet.getId()));
    }

    @Test
    void ownerCanDeleteTweet() throws Exception {

        User owner = createUser();

        Tweet tweet = createTweet(owner, "Delete me");
        Long id = tweet.getId();

        mockMvc.perform(
            delete("/tweet/{id}", id)
                .with(httpBasic(
                    owner.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNoContent());

        assertFalse(tweetRepository.existsById(id));

        mockMvc.perform(
            get("/tweet/findById")
                .param("id", id.toString())
                .with(httpBasic(
                    owner.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNotFound());
    }

    @Test
    void anonymousUserCannotCreateTweet() throws Exception {

        mockMvc.perform(
            post("/tweet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("Anonymous"))
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void blankTweetIsRejected() throws Exception {

        User user = createUser();

        mockMvc.perform(
            post("/tweet")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("   "))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void invalidAndMissingIdsAreHandled() throws Exception {

        User user = createUser();

        mockMvc.perform(
            get("/tweet/findById")
                .param("id", "9000000000000000")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            get("/tweet/findById")
                .param("id", "0")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isBadRequest());

        mockMvc.perform(
            get("/tweet/findById")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isBadRequest());

        mockMvc.perform(
            get("/tweet/findByUserId")
                .param("userId", "abc")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void unknownUserTweetsAreNotFound() throws Exception {

        User user = createUser();

        mockMvc.perform(
            get("/tweet/findByUserId")
                .param("userId", "9000000000000000")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNotFound());
    }

    @Test
    void tweetLongerThan280CharactersIsRejected() throws Exception {

        User user = createUser();

        mockMvc.perform(
            post("/tweet")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json("x".repeat(281)))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void requestMappingErrorsKeepTheirStatus() throws Exception {

        User user = createUser();

        mockMvc.perform(
            get("/tweet/1")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        )
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));

        mockMvc.perform(
            get("/unknown")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));

        mockMvc.perform(
            post("/tweet")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.TEXT_PLAIN)
                .content("plain text")
        )
            .andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.status").value(415));
    }
}
