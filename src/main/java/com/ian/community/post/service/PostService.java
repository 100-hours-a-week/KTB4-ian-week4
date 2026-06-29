package com.ian.community.post.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostImage;
import com.ian.community.post.domain.PostView;
import com.ian.community.post.repository.PostImageRepository;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.post.repository.PostViewRepository;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostViewRepository postViewRepository;

    @Transactional
    public Long createPost(Long userId, String title, String content, String imageUrl) {
        User user = getActiveUser(userId);

        Post post = new Post(user, title, content);
        Post savedPost = postRepository.save(post);

        if (imageUrl != null && !imageUrl.isBlank()) {
            PostImage postImage = new PostImage(savedPost, imageUrl);
            postImageRepository.save(postImage);
        }

        return savedPost.getPostId();
    }

    public Page<Post> getPosts(Pageable pageable) {
        return postRepository.findAllByPostDeletedFalse(pageable);
    }

    public Page<Post> getPostsByUser(Long userId, Pageable pageable) {
        getActiveUser(userId);

        return postRepository.findAllByAuthorUser_UserIdAndPostDeletedFalse(
                userId,
                pageable
        );
    }

    @Transactional
    public Post getPostDetail(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        increaseViewCountIfAllowed(user, post);

        return post;
    }

    @Transactional
    public void updatePost(Long userId, Long postId, String title, String content, String imageUrl) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        validatePostOwner(post, user);

        boolean sameTitle = post.getTitle().equals(title);
        boolean sameContent = post.getContent().equals(content);

        if (sameTitle && sameContent) {
            throw new CustomException(ErrorCode.NO_CHANGES_DETECTED);
        }

        post.update(title, content);

        if (imageUrl != null && !imageUrl.isBlank()) {
            updatePostImage(post, imageUrl);
        }
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        validatePostOwner(post, user);

        if (post.isPostDeleted()) {
            throw new CustomException(ErrorCode.POST_ALREADY_DELETED);
        }

        post.delete();

        if (postImageRepository.existsByAuthorPost(post)) {
            postImageRepository.deleteByAuthorPost(post);
        }
    }

    private void increaseViewCountIfAllowed(User user, Post post) {
        PostView postView = postViewRepository
                .findByAuthorUserAndAuthorPost(user, post)
                .orElse(null);

        if (postView == null) {
            PostView newPostView = new PostView(user, post);
            postViewRepository.save(newPostView);
            post.increaseViewCount();
            return;
        }

        LocalDateTime twentyFourHoursAgo = LocalDateTime.now().minusHours(24);

        if (postView.getViewedAt().isBefore(twentyFourHoursAgo)) {
            postView.updateViewedAt();
            post.increaseViewCount();
        }
    }

    private void updatePostImage(Post post, String imageUrl) {
        PostImage postImage = postImageRepository
                .findByAuthorPost(post)
                .orElse(null);

        if (postImage == null) {
            PostImage newPostImage = new PostImage(post, imageUrl);
            postImageRepository.save(newPostImage);
            return;
        }

        postImage.updateImageUrl(imageUrl);
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

    private void validatePostOwner(Post post, User user) {
        if (!post.getAuthorUser().getUserId().equals(user.getUserId())) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
    }
}