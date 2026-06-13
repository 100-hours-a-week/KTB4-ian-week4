package com.ian.community.post.repository;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.post.domain.Post;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class PostRepository {
    private static final Map<Long, Post> posts = new HashMap<>();
    private Long sequence = 1L;

    public Post save(String title, String content, String authorName, String profileImage, String imageUrl) {
        Post post = new Post(sequence, title, content, authorName, profileImage, imageUrl);
        posts.put(sequence, post);
        sequence++;
        return post;
    }

    public List<Post> findAll() {
        return new ArrayList<>(posts.values());
    }

    public static Optional<Post> findById(Long postId) {
        return Optional.ofNullable(posts.get(postId));
    }

    public void delete(Long postId) {
        Post post = PostRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (post.isPostDeleted()) {
            throw new CustomException(ErrorCode.POST_ALREADY_DELETED);
        }

        post.delete();
    }
}
