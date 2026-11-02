package com.workintech.s19challenge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.workintech.s19challenge.entity.Comment;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.CommentRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommentFlowIT {

    private static final String PASSWORD =
        "medium-comment-test-password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TweetRepository tweetRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User createUser() {

        User user = new User();

        user.setUsername(
            "medium_" +
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
        tweet.setContent("Original tweet");

        return tweetRepository.saveAndFlush(tweet);
    }

    private Comment createComment(
            User author,
            Tweet tweet) {

        Comment comment = new Comment();

        comment.setUser(author);
        comment.setTweet(tweet);
        comment.setContent("Original comment");

        return commentRepository.saveAndFlush(comment);
    }

    private String createJson(
            Long tweetId,
            String content) throws Exception {

        return objectMapper.writeValueAsString(
            Map.of(
                "tweetId", tweetId,
                "content", content
            )
        );
    }

    private String updateJson(String content)
            throws Exception {

        return objectMapper.writeValueAsString(
            Map.of("content", content)
        );
    }

    @Test
    void createsCommentForAuthenticatedUser()
            throws Exception {

        User tweetOwner = createUser();
        User commenter = createUser();
        Tweet tweet = createTweet(tweetOwner);

        String request = objectMapper.writeValueAsString(
            Map.of(
                "tweetId", tweet.getId(),
                "content", "Hello",
                "userId", tweetOwner.getId()
            )
        );

        var result = mockMvc.perform(
            post("/comment/")
                .with(httpBasic(
                    commenter.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.tweetId").value(tweet.getId())
            )
            .andExpect(
                jsonPath("$.userId").value(commenter.getId())
            )
            .andExpect(
                jsonPath("$.content").value("Hello")
            )
            .andReturn();

        Long id = objectMapper.readTree(
            result.getResponse().getContentAsString()
        ).get("id").asLong();

        Comment saved = commentRepository
            .findById(id)
            .orElseThrow();

        assertEquals(
            commenter.getId(),
            saved.getUser().getId()
        );

        assertEquals(
            tweet.getId(),
            saved.getTweet().getId()
        );
    }

    @Test
    void anotherUserCanCommentOnTweet()
            throws Exception {

        User owner = createUser();
        User commenter = createUser();
        Tweet tweet = createTweet(owner);

        mockMvc.perform(
            post("/comment")
                .with(httpBasic(
                    commenter.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createJson(tweet.getId(), "A reply")
                )
        )
            .andExpect(status().isCreated())
            .andExpect(
                jsonPath("$.username")
                    .value(commenter.getUsername())
            );
    }

    @Test
    void unknownTweetReturns404()
            throws Exception {

        User commenter = createUser();

        mockMvc.perform(
            post("/comment")
                .with(httpBasic(
                    commenter.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createJson(
                        9000000000000000L,
                        "Comment"
                    )
                )
        ).andExpect(status().isNotFound());
    }

    @Test
    void invalidCommentInputReturns400()
            throws Exception {

        User commenter = createUser();
        Tweet tweet = createTweet(commenter);

        mockMvc.perform(
            post("/comment")
                .with(httpBasic(
                    commenter.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createJson(tweet.getId(), "   ")
                )
        ).andExpect(status().isBadRequest());

        mockMvc.perform(
            post("/comment")
                .with(httpBasic(
                    commenter.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    updateJson("Missing tweet ID")
                )
        ).andExpect(status().isBadRequest());

        mockMvc.perform(
            post("/comment")
                .with(httpBasic(
                    commenter.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createJson(0L, "Invalid tweet ID")
                )
        ).andExpect(status().isBadRequest());
    }

    @Test
    void commentAuthorCanUpdate()
            throws Exception {

        User author = createUser();
        Tweet tweet = createTweet(author);
        Comment comment = createComment(author, tweet);

        mockMvc.perform(
            put("/comment/{id}", comment.getId())
                .with(httpBasic(
                    author.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson("Updated comment"))
        )
            .andExpect(status().isOk())
            .andExpect(
                jsonPath("$.content")
                    .value("Updated comment")
            );

        assertEquals(
            "Updated comment",
            commentRepository
                .findById(comment.getId())
                .orElseThrow()
                .getContent()
        );
    }

    @Test
    void anotherUserCannotUpdate()
            throws Exception {

        User author = createUser();
        User other = createUser();
        Tweet tweet = createTweet(author);
        Comment comment = createComment(author, tweet);

        mockMvc.perform(
            put("/comment/{id}", comment.getId())
                .with(httpBasic(
                    other.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson("Unauthorized"))
        ).andExpect(status().isForbidden());

        assertEquals(
            "Original comment",
            commentRepository
                .findById(comment.getId())
                .orElseThrow()
                .getContent()
        );
    }

    @Test
    void commentAuthorCanDelete()
            throws Exception {

        User tweetOwner = createUser();
        User commenter = createUser();

        Tweet tweet = createTweet(tweetOwner);
        Comment comment = createComment(commenter, tweet);

        mockMvc.perform(
            delete("/comment/{id}", comment.getId())
                .with(httpBasic(
                    commenter.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNoContent());

        assertFalse(
            commentRepository.existsById(comment.getId())
        );
    }

    @Test
    void tweetOwnerCanDeleteAnotherUsersComment()
            throws Exception {

        User tweetOwner = createUser();
        User commenter = createUser();

        Tweet tweet = createTweet(tweetOwner);
        Comment comment = createComment(commenter, tweet);

        mockMvc.perform(
            delete("/comment/{id}", comment.getId())
                .with(httpBasic(
                    tweetOwner.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNoContent());

        assertFalse(
            commentRepository.existsById(comment.getId())
        );
    }

    @Test
    void unrelatedUserCannotDelete()
            throws Exception {

        User tweetOwner = createUser();
        User commenter = createUser();
        User unrelated = createUser();

        Tweet tweet = createTweet(tweetOwner);
        Comment comment = createComment(commenter, tweet);

        mockMvc.perform(
            delete("/comment/{id}", comment.getId())
                .with(httpBasic(
                    unrelated.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isForbidden());

        assertTrue(
            commentRepository.existsById(comment.getId())
        );
    }

    @Test
    void invalidAndUnknownCommentIds()
            throws Exception {

        User user = createUser();

        mockMvc.perform(
            delete("/comment/0")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isBadRequest());

        mockMvc.perform(
            delete("/comment/9000000000000000")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
        ).andExpect(status().isNotFound());

        mockMvc.perform(
            put("/comment/0")
                .with(httpBasic(
                    user.getUsername(),
                    PASSWORD
                ))
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateJson("Test"))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void anonymousUserCannotCreateComment()
            throws Exception {

        mockMvc.perform(
            post("/comment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    createJson(1L, "Anonymous comment")
                )
        ).andExpect(status().isUnauthorized());
    }
}
