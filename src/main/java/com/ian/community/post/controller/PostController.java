package com.ian.community.post.controller;

import com.ian.community.common.ApiResponse;
import com.ian.community.post.domain.Post;
import com.ian.community.post.dto.request.PostCommentCreateRequest;
import com.ian.community.post.dto.request.PostCommentUpdateRequest;
import com.ian.community.post.dto.request.PostCreateRequest;
import com.ian.community.post.dto.request.PostUpdateRequest;
import com.ian.community.post.dto.response.PostCommentResponse;
import com.ian.community.post.dto.response.PostDetailResponse;
import com.ian.community.post.dto.response.PostLikeResponse;
import com.ian.community.post.dto.response.PostResponse;
import com.ian.community.post.service.CommentService;
import com.ian.community.post.service.PostLikeService;
import com.ian.community.post.service.PostService;
import com.ian.community.security.principal.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;

    public PostController(PostService postService, CommentService commentService,  PostLikeService postLikeService) {
        this.postService = postService;
        this.commentService = commentService;
        this.postLikeService = postLikeService;
    }

    @PostMapping("/{userId}")
    public ResponseEntity<Long> createPost(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid  @RequestBody PostCreateRequest request
    ) {
        Long postId = postService.createPost(
                authenticatedUser.getUserId(),
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
                .map(post -> new PostResponse(post, postService.getPostImageUrl(post)));

        return ResponseEntity
                .ok(new ApiResponse<>("post_list_found", response));
    }

    // 게시물 상세 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPostDetail(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId
    ) {
        Post post = postService.getPostDetail(authenticatedUser.getUserId(), postId);

        List<PostCommentResponse> comments = commentService
                .getComments(postId, Pageable.unpaged())
                .map(PostCommentResponse::from)
                .getContent();

        return ResponseEntity.ok(PostDetailResponse.from(
                post,
                comments,
                postService.getPostImageUrl(post),
                postLikeService.isLiked(authenticatedUser.getUserId(), postId)
        ));
    }

    // 게시물 수정
    @PatchMapping("/{postId}")
    public ResponseEntity<Void> updatePost(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateRequest request
    ) {
        postService.updatePost(
                authenticatedUser.getUserId(),
                postId,
                request.getTitle(),
                request.getContent(),
                request.getImageUrl()
        );

        return ResponseEntity.noContent().build();
    }

    // 게시물 삭제
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") Long userId
    ) {
        postService.deletePost(userId, postId);

        return ResponseEntity.noContent().build();
    }

    // 게시글 좋아요
    @PostMapping("/{postId}/likes")
    public ResponseEntity<PostLikeResponse> toggleLike(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "1") Long userId
    ) {
        boolean liked = postLikeService.toggleLike(userId, postId);
        int likeCount = Math.toIntExact(postLikeService.countLikes(postId));

        return ResponseEntity.ok(new PostLikeResponse(postId, liked, likeCount));
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments/users/{userId}")
    public ResponseEntity<Long> createComment(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody PostCommentCreateRequest request
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
            @Valid @RequestBody PostCommentUpdateRequest request
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

    // 댓글 삭제
    @DeleteMapping("/{postId}/comments/{commentId}/users/{userId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(userId, postId, commentId);

        return ResponseEntity.noContent().build();
    }
}
