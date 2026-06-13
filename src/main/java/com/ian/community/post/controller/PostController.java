package com.ian.community.post.controller;

import com.ian.community.common.ApiResponse;
import com.ian.community.post.domain.Post;
import com.ian.community.post.dto.PostCreateRequest;
import com.ian.community.post.dto.PostResponse;
import com.ian.community.post.dto.PostUpdateRequest;
import com.ian.community.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.beans.Transient;
import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // 게시물 작성
    @PostMapping
    @Transient
    public ResponseEntity<ApiResponse<PostResponse>> create(
            @Valid @RequestBody PostCreateRequest request
    ) {
        Long userId = 1L;

        Post post = postService.create(
                request.getTitle(),
                request.getContent(),
                request.getImageUrl()
        );

        PostResponse response = new PostResponse(post);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>("post_created", response));
    }

    // 게시물 목록 조회
    @GetMapping
    @Transient
    public ResponseEntity<ApiResponse<List<PostResponse>>> findAll() {
        List<PostResponse> response = postService.findAll()
                .stream()
                .map(PostResponse::new)
                .toList();

        return ResponseEntity
                .ok(new ApiResponse<>("post_list_found", response));
    }

    // 게시물 상세 조회
    @GetMapping("/{postId}")
    @Transient
    public ResponseEntity<ApiResponse<PostResponse>> findById(
            @PathVariable Long postId
    ) {
        Post post = postService.findById(postId);

        PostResponse response = new PostResponse(post);

        return ResponseEntity
                .ok(new ApiResponse<>("post_found", response));
    }

    // 게시물 수정
    @PatchMapping("/{postId}")
    @Transient
    public ResponseEntity<ApiResponse<PostResponse>> update(
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        Post post = postService.update(
                postId,
                request.getTitle(),
                request.getContent(),
                request.getImageUrl()
        );

        PostResponse response = new PostResponse(post);

        return ResponseEntity
                .ok(new ApiResponse<>("post_updated", response));
    }

    // 게시물 삭제
    @DeleteMapping("/{postId}")
    @Transient
    public ResponseEntity<ApiResponse<Boolean>> delete(
            @PathVariable Long postId
    ) {
        postService.delete(postId);

        return ResponseEntity
                .ok(new ApiResponse<>("post_deleted", true));
    }
}
