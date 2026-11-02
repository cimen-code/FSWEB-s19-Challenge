package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.*;
import com.workintech.s19challenge.entity.*;
import com.workintech.s19challenge.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TweetRepository tweetRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TweetLikeRepository tweetLikeRepository;

    @Mock
    private RetweetRepository retweetRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private TweetServiceImpl tweetService;

    @InjectMocks
    private CommentServiceImpl commentService;

    @InjectMocks
    private TweetLikeServiceImpl tweetLikeService;

    @InjectMocks
    private RetweetServiceImpl retweetService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    private User user(long id, String username) {

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("encoded-password");

        return user;
    }

    private Tweet tweet(User owner) {

        Tweet tweet = new Tweet();
        tweet.setId(10L);
        tweet.setUser(owner);
        tweet.setContent("Original tweet");

        return tweet;
    }

    private Comment comment(User author, Tweet tweet) {

        Comment comment = new Comment();
        comment.setId(20L);
        comment.setUser(author);
        comment.setTweet(tweet);
        comment.setContent("Original comment");

        return comment;
    }

    private TweetLike tweetLike(User user, Tweet tweet) {

        TweetLike tweetLike = new TweetLike();
        tweetLike.setId(30L);
        tweetLike.setUser(user);
        tweetLike.setTweet(tweet);

        return tweetLike;
    }

    private Retweet retweet(User user, Tweet tweet) {

        Retweet retweet = new Retweet();
        retweet.setId(40L);
        retweet.setUser(user);
        retweet.setTweet(tweet);

        return retweet;
    }

    // 1. TweetService.create

    @Test
    void tweetCreate() {

        User alice = user(1L, "alice");

        when(userRepository.findByUsername("alice"))
            .thenReturn(Optional.of(alice));

        when(tweetRepository.saveAndFlush(any(Tweet.class)))
            .thenAnswer(invocation -> {

                Tweet saved = invocation.getArgument(0);
                saved.setId(10L);

                return saved;
            });

        TweetResponse result = tweetService.create(
            "alice",
            new TweetRequest("Hello")
        );

        assertEquals(10L, result.id());
        assertEquals(1L, result.userId());
        assertEquals("Hello", result.content());

        verify(tweetRepository)
            .saveAndFlush(any(Tweet.class));
    }

    // 2. TweetService.findByUserId

    @Test
    void tweetFindByUserId() {

        User alice = user(1L, "alice");

        when(userRepository.existsById(1L))
            .thenReturn(true);

        when(tweetRepository.findByUser_Id(1L))
            .thenReturn(List.of(tweet(alice)));

        List<TweetResponse> result =
            tweetService.findByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).userId());
    }

    // 3. TweetService.findById

    @Test
    void tweetFindById() {

        Tweet existing = tweet(user(1L, "alice"));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(existing));

        TweetResponse result =
            tweetService.findById(10L);

        assertEquals(10L, result.id());
        assertEquals("Original tweet", result.content());
    }

    // 4. TweetService.update

    @Test
    void tweetUpdate() {

        Tweet existing = tweet(user(1L, "alice"));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(existing));

        when(tweetRepository.saveAndFlush(any(Tweet.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        TweetResponse result = tweetService.update(
            10L,
            "alice",
            new TweetRequest("Updated tweet")
        );

        assertEquals("Updated tweet", result.content());
        assertEquals("Updated tweet", existing.getContent());
    }

    // 5. TweetService.delete

    @Test
    void tweetDelete() {

        Tweet existing = tweet(user(1L, "alice"));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(existing));

        tweetService.delete(10L, "alice");

        verify(tweetRepository).delete(existing);
        verify(tweetRepository).flush();
    }

    // 6. CommentService.create

    @Test
    void commentCreate() {

        User alice = user(1L, "alice");
        User bob = user(2L, "bob");

        Tweet original = tweet(alice);

        when(userRepository.findByUsername("bob"))
            .thenReturn(Optional.of(bob));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(original));

        when(commentRepository.saveAndFlush(any(Comment.class)))
            .thenAnswer(invocation -> {

                Comment saved = invocation.getArgument(0);
                saved.setId(20L);

                return saved;
            });

        CommentResponse result = commentService.create(
            "bob",
            new CommentRequest(10L, "Nice tweet")
        );

        assertEquals(20L, result.id());
        assertEquals(10L, result.tweetId());
        assertEquals(2L, result.userId());
        assertEquals("Nice tweet", result.content());
    }

    // 7. CommentService.update

    @Test
    void commentUpdate() {

        Comment existing = comment(
            user(2L, "bob"),
            tweet(user(1L, "alice"))
        );

        when(commentRepository.findById(20L))
            .thenReturn(Optional.of(existing));

        when(commentRepository.saveAndFlush(any(Comment.class)))
            .thenAnswer(invocation ->
                invocation.getArgument(0)
            );

        CommentResponse result = commentService.update(
            20L,
            "bob",
            new CommentUpdateRequest("Edited comment")
        );

        assertEquals("Edited comment", result.content());
        assertEquals("Edited comment", existing.getContent());
    }

    // 8. CommentService.delete

    @Test
    void commentDeleteByTweetOwner() {

        User alice = user(1L, "alice");
        User bob = user(2L, "bob");

        Comment existing = comment(
            bob,
            tweet(alice)
        );

        when(commentRepository.findById(20L))
            .thenReturn(Optional.of(existing));

        commentService.delete(20L, "alice");

        verify(commentRepository).delete(existing);
        verify(commentRepository).flush();
    }

    // 9. TweetLikeService.like

    @Test
    void likeCreate() {

        User alice = user(1L, "alice");
        User bob = user(2L, "bob");

        when(userRepository.findByUsername("bob"))
            .thenReturn(Optional.of(bob));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(tweet(alice)));

        when(tweetLikeRepository.saveAndFlush(any(TweetLike.class)))
            .thenAnswer(invocation -> {

                TweetLike saved = invocation.getArgument(0);
                saved.setId(30L);

                return saved;
            });

        TweetLikeResponse result =
            tweetLikeService.like("bob", 10L);

        assertEquals(30L, result.id());
        assertEquals(10L, result.tweetId());
        assertEquals(2L, result.userId());
    }

    // 10. TweetLikeService.dislike

    @Test
    void likeRemove() {

        User alice = user(1L, "alice");
        User bob = user(2L, "bob");

        TweetLike existing = tweetLike(
            bob,
            tweet(alice)
        );

        when(userRepository.findByUsername("bob"))
            .thenReturn(Optional.of(bob));

        when(tweetLikeRepository.findByUser_IdAndTweet_Id(
                2L, 10L))
            .thenReturn(Optional.of(existing));

        tweetLikeService.dislike("bob", 10L);

        verify(tweetLikeRepository).delete(existing);
        verify(tweetLikeRepository).flush();
    }

    // 11. RetweetService.create

    @Test
    void retweetCreate() {

        User alice = user(1L, "alice");
        User bob = user(2L, "bob");

        when(userRepository.findByUsername("bob"))
            .thenReturn(Optional.of(bob));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(tweet(alice)));

        when(retweetRepository.saveAndFlush(any(Retweet.class)))
            .thenAnswer(invocation -> {

                Retweet saved = invocation.getArgument(0);
                saved.setId(40L);

                return saved;
            });

        RetweetResponse result =
            retweetService.create("bob", 10L);

        assertEquals(40L, result.id());
        assertEquals(10L, result.tweetId());
        assertEquals(2L, result.userId());
    }

    // 12. RetweetService.delete

    @Test
    void retweetDelete() {

        Retweet existing = retweet(
            user(2L, "bob"),
            tweet(user(1L, "alice"))
        );

        when(retweetRepository.findById(40L))
            .thenReturn(Optional.of(existing));

        retweetService.delete(40L, "bob");

        verify(retweetRepository).delete(existing);
        verify(retweetRepository).flush();
    }

    // 13. AuthenticationService.register

    @Test
    void authenticationRegister() {

        when(userRepository.findByUsername("alice"))
            .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
            .thenReturn("encoded-password");

        when(userRepository.saveAndFlush(any(User.class)))
            .thenAnswer(invocation -> {

                User saved = invocation.getArgument(0);
                saved.setId(1L);

                return saved;
            });

        AuthResponse result = authenticationService.register(
            new AuthRequest("alice", "password123")
        );

        assertEquals(1L, result.id());
        assertEquals("alice", result.username());

        verify(passwordEncoder).encode("password123");
    }

    // 14. AuthenticationService.login

    @Test
    void authenticationLogin() {

        User alice = user(1L, "alice");

        when(userRepository.findByUsername("alice"))
            .thenReturn(Optional.of(alice));

        AuthResponse result = authenticationService.login(
            new AuthRequest("alice", "password123")
        );

        assertEquals(1L, result.id());
        assertEquals("alice", result.username());

        verify(authenticationManager).authenticate(
            argThat(authentication ->
                "alice".equals(authentication.getName())
                && "password123".equals(
                    authentication.getCredentials()
                )
            )
        );
    }

    // 15. UserDetailsService.loadUserByUsername

    @Test
    void userDetailsLoad() {

        User alice = user(1L, "alice");

        when(userRepository.findByUsername("alice"))
            .thenReturn(Optional.of(alice));

        UserDetails result =
            userDetailsService.loadUserByUsername("alice");

        assertEquals("alice", result.getUsername());

        assertEquals(
            "encoded-password",
            result.getPassword()
        );

        assertTrue(
            result.getAuthorities()
                .stream()
                .anyMatch(authority ->
                    authority.getAuthority().equals("USER")
                )
        );
    }

    // 16. Authorization and error branches

    private void assertStatus(
            HttpStatus expected,
            Executable action) {

        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            action
        );

        assertEquals(expected, exception.getStatusCode());
    }

    @Test
    void tweetUpdateByOtherUserIsForbidden() {

        Tweet existing = tweet(user(1L, "alice"));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(existing));

        assertStatus(
            HttpStatus.FORBIDDEN,
            () -> tweetService.update(
                10L,
                "bob",
                new TweetRequest("Changed")
            )
        );

        assertEquals("Original tweet", existing.getContent());
        verify(tweetRepository, never()).saveAndFlush(any());
    }

    @Test
    void tweetDeleteByOtherUserIsForbidden() {

        Tweet existing = tweet(user(1L, "alice"));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(existing));

        assertStatus(
            HttpStatus.FORBIDDEN,
            () -> tweetService.delete(10L, "bob")
        );

        verify(tweetRepository, never()).delete(any());
    }

    @Test
    void tweetFindByIdUnknownIsNotFound() {

        when(tweetRepository.findById(99L))
            .thenReturn(Optional.empty());

        assertStatus(
            HttpStatus.NOT_FOUND,
            () -> tweetService.findById(99L)
        );
    }

    @Test
    void tweetInvalidIdIsBadRequest() {

        assertStatus(
            HttpStatus.BAD_REQUEST,
            () -> tweetService.findById(0L)
        );

        verifyNoInteractions(tweetRepository);
    }

    @Test
    void tweetFindByUnknownUserIsNotFound() {

        when(userRepository.existsById(99L))
            .thenReturn(false);

        assertStatus(
            HttpStatus.NOT_FOUND,
            () -> tweetService.findByUserId(99L)
        );

        verify(tweetRepository, never()).findByUser_Id(any());
    }

    @Test
    void commentUpdateByTweetOwnerIsForbidden() {

        Comment existing = comment(
            user(2L, "bob"),
            tweet(user(1L, "alice"))
        );

        when(commentRepository.findById(20L))
            .thenReturn(Optional.of(existing));

        assertStatus(
            HttpStatus.FORBIDDEN,
            () -> commentService.update(
                20L,
                "alice",
                new CommentUpdateRequest("Changed")
            )
        );

        assertEquals("Original comment", existing.getContent());
    }

    @Test
    void commentDeleteByCommentOwner() {

        Comment existing = comment(
            user(2L, "bob"),
            tweet(user(1L, "alice"))
        );

        when(commentRepository.findById(20L))
            .thenReturn(Optional.of(existing));

        commentService.delete(20L, "bob");

        verify(commentRepository).delete(existing);
    }

    @Test
    void commentDeleteByUnrelatedUserIsForbidden() {

        Comment existing = comment(
            user(2L, "bob"),
            tweet(user(1L, "alice"))
        );

        when(commentRepository.findById(20L))
            .thenReturn(Optional.of(existing));

        assertStatus(
            HttpStatus.FORBIDDEN,
            () -> commentService.delete(20L, "carol")
        );

        verify(commentRepository, never()).delete(any());
    }

    @Test
    void likeTwiceIsConflict() {

        when(userRepository.findByUsername("bob"))
            .thenReturn(Optional.of(user(2L, "bob")));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(tweet(user(1L, "alice"))));

        when(tweetLikeRepository.existsByUser_IdAndTweet_Id(2L, 10L))
            .thenReturn(true);

        assertStatus(
            HttpStatus.CONFLICT,
            () -> tweetLikeService.like("bob", 10L)
        );

        verify(tweetLikeRepository, never()).saveAndFlush(any());
    }

    @Test
    void dislikeWithoutLikeIsNotFound() {

        when(userRepository.findByUsername("bob"))
            .thenReturn(Optional.of(user(2L, "bob")));

        when(tweetLikeRepository.findByUser_IdAndTweet_Id(2L, 10L))
            .thenReturn(Optional.empty());

        assertStatus(
            HttpStatus.NOT_FOUND,
            () -> tweetLikeService.dislike("bob", 10L)
        );
    }

    @Test
    void retweetTwiceIsConflict() {

        when(userRepository.findByUsername("bob"))
            .thenReturn(Optional.of(user(2L, "bob")));

        when(tweetRepository.findById(10L))
            .thenReturn(Optional.of(tweet(user(1L, "alice"))));

        when(retweetRepository.existsByUser_IdAndTweet_Id(2L, 10L))
            .thenReturn(true);

        assertStatus(
            HttpStatus.CONFLICT,
            () -> retweetService.create("bob", 10L)
        );

        verify(retweetRepository, never()).saveAndFlush(any());
    }

    @Test
    void retweetDeleteByOtherUserIsForbidden() {

        Retweet existing = retweet(
            user(2L, "bob"),
            tweet(user(1L, "alice"))
        );

        when(retweetRepository.findById(40L))
            .thenReturn(Optional.of(existing));

        assertStatus(
            HttpStatus.FORBIDDEN,
            () -> retweetService.delete(40L, "alice")
        );

        verify(retweetRepository, never()).delete(any());
    }

    @Test
    void registerDuplicateUsernameIsConflict() {

        when(userRepository.findByUsername("alice"))
            .thenReturn(Optional.of(user(1L, "alice")));

        assertStatus(
            HttpStatus.CONFLICT,
            () -> authenticationService.register(
                new AuthRequest("alice", "password123")
            )
        );

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void loginWithWrongPasswordIsUnauthorized() {

        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        assertStatus(
            HttpStatus.UNAUTHORIZED,
            () -> authenticationService.login(
                new AuthRequest("alice", "wrong-password")
            )
        );
    }

    @Test
    void unknownUserDetailsAreRejected() {

        when(userRepository.findByUsername("ghost"))
            .thenReturn(Optional.empty());

        assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername("ghost")
        );
    }
}
