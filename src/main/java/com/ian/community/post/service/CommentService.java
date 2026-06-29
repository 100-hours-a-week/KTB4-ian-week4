package com.ian.community.post.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostComment;
import com.ian.community.post.repository.CommentRepository;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public Long createComment(Long userId, Long postId, String commentContent) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        if (!post.getCommentable()) {
            throw new CustomException(ErrorCode.INVALID_COMMENT_REQUEST);
        }

        PostComment comment = new PostComment(user, post, commentContent);

        PostComment savedComment = commentRepository.save(comment);
        post.increaseCommentCount();

        return savedComment.getCommentId();
    }

    public Slice<PostComment> getComments(Long postId, Pageable pageable) {
        Post post = getActivePost(postId);

        return commentRepository.findAllByAuthorPostAndCommentDeletedFalse(post, pageable);
    }

    // 필요 없을 시에 삭제
    @Transactional
    public void updateComment(Long userId, Long postId, Long commentId, String commentContent) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        PostComment comment = commentRepository
                .findByAuthorPostAndCommentIdAndCommentDeletedFalse(post, commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        validateCommentOwner(comment, user);

        if (comment.getComment().equals(commentContent)) {
            throw new CustomException(ErrorCode.NO_CHANGES_DETECTED);
        }

        comment.updateContent(commentContent);
    }

    @Transactional
    public void deleteComment(Long userId, Long postId, Long commentId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        PostComment comment = commentRepository
                .findByAuthorPostAndCommentIdAndCommentDeletedFalse(post, commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        validateCommentOwner(comment, user);

        if (comment.getCommentDeleted()) {
            throw new CustomException(ErrorCode.COMMENT_ALREADY_DELETED);
        }

        comment.delete();
        post.decreaseCommentCount();
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isUserDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        return user;
    }

    private Post getActivePost(Long postId) {
        return postRepository.findByPostIdAndPostDeletedFalse(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }

    private void validateCommentOwner(PostComment comment, User user) {
        if (!comment.getAuthorUser().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}