package com.workintech.s19challenge.service;

import com.workintech.s19challenge.dto.CommentRequest;
import com.workintech.s19challenge.dto.CommentResponse;
import com.workintech.s19challenge.dto.CommentUpdateRequest;
import com.workintech.s19challenge.entity.Comment;
import com.workintech.s19challenge.entity.Tweet;
import com.workintech.s19challenge.entity.User;
import com.workintech.s19challenge.repository.CommentRepository;
import com.workintech.s19challenge.repository.TweetRepository;
import com.workintech.s19challenge.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final TweetRepository tweetRepository;
    private final UserRepository userRepository;

    public CommentServiceImpl(
            CommentRepository commentRepository,
            TweetRepository tweetRepository,
            UserRepository userRepository) {

        this.commentRepository = commentRepository;
        this.tweetRepository = tweetRepository;
        this.userRepository = userRepository;
    }

    @Override
    public CommentResponse create(
            String username,
            CommentRequest request) {

        User user = userRepository
            .findByUsername(username)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "User not found"
                )
            );

        Tweet tweet = tweetRepository
            .findById(request.tweetId())
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Tweet not found"
                )
            );

        Comment comment = new Comment();

        comment.setUser(user);
        comment.setTweet(tweet);
        comment.setContent(request.content());

        return toResponse(
            commentRepository.saveAndFlush(comment)
        );
    }

    @Override
    public CommentResponse update(
            Long id,
            String username,
            CommentUpdateRequest request) {

        Comment comment = findComment(id);

        if (!comment.getUser()
                .getUsername()
                .equals(username)) {

            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the comment owner can update it"
            );
        }

        comment.setContent(request.content());

        return toResponse(
            commentRepository.saveAndFlush(comment)
        );
    }

    @Override
    public void delete(Long id, String username) {

        Comment comment = findComment(id);

        boolean commentOwner = comment.getUser()
            .getUsername()
            .equals(username);

        boolean tweetOwner = comment.getTweet()
            .getUser()
            .getUsername()
            .equals(username);

        if (!commentOwner && !tweetOwner) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the comment owner or tweet owner can delete it"
            );
        }

        commentRepository.delete(comment);
        commentRepository.flush();
    }

    private Comment findComment(Long id) {

        if (id <= 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "ID must be positive"
            );
        }

        return commentRepository
            .findById(id)
            .orElseThrow(() ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Comment not found"
                )
            );
    }

    private CommentResponse toResponse(Comment comment) {

        return new CommentResponse(
            comment.getId(),
            comment.getTweet().getId(),
            comment.getUser().getId(),
            comment.getUser().getUsername(),
            comment.getContent()
        );
    }
}
