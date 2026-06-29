package com.ian.community.post.controller;

import com.ian.community.common.ApiResponse;
import com.ian.community.post.domain.Post;
import com.ian.community.post.dto.request.PostCommentCreateRequest;
import com.ian.community.post.dto.request.PostCommentUpdateRequest;
import com.ian.community.post.dto.request.PostCreateRequest;
import com.ian.community.post.dto.request.PostUpdateRequest;
import com.ian.community.post.dto.response.PostDetailResponse;
import com.ian.community.post.dto.response.PostResponse;
import com.ian.community.post.service.CommentService;
import com.ian.community.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;

    public PostController(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }
    @PostMapping
    public ResponseEntity<Long> createPost(
            @PathVariable Long userId,
            @RequestBody PostCreateRequest request
    ) {
        Long postId = postService.createPost(
                userId,
                request.getTitle(),
                request.getContent(),
                request.getImageUrl()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    // 게시물 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostResponse>>> findAll(Pageable pageable) {
        Page<PostResponse> response = postService.getPosts(pageable)
                .map(PostResponse::new);

        return ResponseEntity
                .ok(new ApiResponse<>("post_list_found", response));
    }

    // 게시물 상세 조회
    @GetMapping("{post_id}")
    public ResponseEntity<PostDetailResponse> getPostDetail(
            @PathVariable Long userId,
            @PathVariable Long postId
    ) {
        Post post = postService.getPostDetail(userId, postId);

        return ResponseEntity.ok(PostDetailResponse.from(post));
    }

    // 게시물 수정
    @PatchMapping("{post_id}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @RequestBody PostUpdateRequest request
    ) {
        postService.updatePost(
                userId,
                postId,
                request.getTitle(),
                request.getContent(),
                request.getImageUrl()
        );

        return ResponseEntity.noContent().build();
    }

    // 게시물 삭제
    @DeleteMapping("{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long userId,
            @PathVariable Long postId
    ) {
        postService.deletePost(userId, postId);

        return ResponseEntity.noContent().build();
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments/users/{userId}")
    public ResponseEntity<Long> createComment(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @RequestBody PostCommentCreateRequest request
    ) {
        Long commentId = commentService.createComment(
                userId,
                postId,
                request.getComment()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(commentId);
    }

    // 댓글 수정
    @PatchMapping("/{postId}/comments/{commentId}/users/{userId}")
    public ResponseEntity<Void> updateComment(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestBody PostCommentUpdateRequest request
    ) {
        commentService.updateComment(
                userId,
                postId,
                commentId,
                request.getComment()
        );

        return ResponseEntity
                .noContent()
                .build();
    }
}
