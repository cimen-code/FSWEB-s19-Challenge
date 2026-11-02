package com.workintech.s19challenge.repository;

import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.entity.Tweet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
    replace = AutoConfigureTestDatabase.Replace.NONE
)
class EntityRelationshipIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TweetRepository tweetRepository;

    private User createUser() {

        User user = new User();

        user.setUsername(
            "test_" + UUID.randomUUID()
                .toString()
                .replace("-", "")
        );

        user.setPasswordHash("test-only-hash");

        return userRepository.saveAndFlush(user);
    }

    @Test
    void savesUserAndFindsByUsername() {

        User user = createUser();

        User found = userRepository
            .findByUsername(user.getUsername())
            .orElseThrow();

        assertEquals(user.getId(), found.getId());
    }

    @Test
    void savesTweetWithItsAuthor() {

        User user = createUser();

        Tweet tweet = new Tweet();
        tweet.setUser(user);
        tweet.setContent("Spring Boot challenge test");

        Tweet saved = tweetRepository.saveAndFlush(tweet);

        assertNotNull(saved.getId());
        assertEquals(
            user.getId(),
            saved.getUser().getId()
        );
    }

    @Test
    void findsTweetsOnlyForRequestedUser() {

        User firstUser = createUser();
        User secondUser = createUser();

        Tweet firstTweet = new Tweet();
        firstTweet.setUser(firstUser);
        firstTweet.setContent("First user's tweet");

        Tweet secondTweet = new Tweet();
        secondTweet.setUser(secondUser);
        secondTweet.setContent("Second user's tweet");

        tweetRepository.saveAndFlush(firstTweet);
        tweetRepository.saveAndFlush(secondTweet);

        List<Tweet> results =
            tweetRepository.findByUser_Id(firstUser.getId());

        assertEquals(1, results.size());

        assertEquals(
            firstUser.getId(),
            results.get(0).getUser().getId()
        );
    }
}
