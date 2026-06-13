package com.ian.community.post.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.post.domain.Post;
import com.ian.community.post.repository.PostRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PostService {
    private PostRepository postRepository;

    public PostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    public Post create(String title, String content, String imageUrl) {
        validatePostRequest(title, content);

        String authorName = "테스트유저";
        String profileImage = "https://image.kr/default-profile.jpg";

        return postRepository.save(title, content, authorName, profileImage, imageUrl);
    }

    public List<Post> findAll() {
        return postRepository.findAll();
    }

    public Post findById(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        post.increaseViewCount();

        return post;
    }

    public Post update(Long postId, String title, String content, String imageUrl) {
        validatePostRequest(title, content);

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        post.update(title, content, imageUrl);

        return post;
    }

    public void delete(Long postId) {
        postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        postRepository.delete(postId);
    }

    private void validatePostRequest(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_POST_REQUEST);
        }

        if (content == null || content.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_POST_REQUEST);
        }
    }
}
