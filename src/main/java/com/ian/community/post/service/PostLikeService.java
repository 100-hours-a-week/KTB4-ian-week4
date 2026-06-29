package com.ian.community.post.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostLike;
import com.ian.community.post.repository.PostLikeRepository;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;

    @Transactional
    public boolean toggleLike(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        Optional<PostLike> postLikeOptional =
                postLikeRepository.findByAuthorUserAndAuthorPost(user, post);

        if (postLikeOptional.isPresent()) {
            postLikeRepository.delete(postLikeOptional.get());
            post.decreaseLikeCount();
            return false;
        }

        PostLike postLike = new PostLike(user, post);
        postLikeRepository.save(postLike);
        post.increaseLikeCount();

        return true;
    }

    public long countLikes(Long postId) {
        Post post = getActivePost(postId);

        return postLikeRepository.countByAuthorPost(post);
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
}