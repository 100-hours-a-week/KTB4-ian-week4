package com.ian.community.post.controller;

import com.ian.community.common.ApiResponse;
import com.ian.community.common.image.LocalImageStorageService;
import com.ian.community.post.domain.Post;
import com.ian.community.post.dto.request.PostCommentCreateRequest;
import com.ian.community.post.dto.request.PostCommentUpdateRequest;
import com.ian.community.post.dto.request.PostCreateRequest;
import com.ian.community.post.dto.request.PostUpdateRequest;
import com.ian.community.post.dto.response.*;
import com.ian.community.post.service.CommentService;
import com.ian.community.post.service.BookmarkService;
import com.ian.community.post.service.PostLikeService;
import com.ian.community.post.service.PostService;
import com.ian.community.security.principal.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final PostLikeService postLikeService;
    private final BookmarkService bookmarkService;
    private final LocalImageStorageService imageStorageService;

    public PostController(
            PostService postService,
            CommentService commentService,
            PostLikeService postLikeService,
            BookmarkService bookmarkService,
            LocalImageStorageService imageStorageService
    ) {
        this.postService = postService;
        this.commentService = commentService;
        this.postLikeService = postLikeService;
        this.bookmarkService = bookmarkService;
        this.imageStorageService = imageStorageService;
    }

    @PostMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Long> createPost(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid  @RequestBody PostCreateRequest request
    ) {
        Long postId = postService.createPost(
                authenticatedUser.getUserId(),
                request.getContent(),
                request.getImageUrl()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    @PostMapping(value = "/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Long> createPostWithImage(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @RequestPart("content") String content,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        String imageUrl = image == null ? null : imageStorageService.storeFeed(image);
        Long postId = postService.createPost(
                authenticatedUser.getUserId(),
                content,
                imageUrl
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(postId);
    }

    // 게시물 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<SliceResponse<PostResponse>>> findAll(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        Pageable limited = limitPageSize(pageable);
        Slice<Post> posts = postService.getPosts(limited);
        Set<Long> bookmarkedPostIds =
                bookmarkService.findBookmarkedPostIds(
                        authenticatedUser.getUserId(),
                        posts.getContent()
                                .stream()
                                .map(Post::getPostId)
                                .toList()
                );

        Slice<PostResponse> response = posts.map(
                post -> new PostResponse(
                        post,
                        postService.getPostImageUrl(post),
                        bookmarkedPostIds.contains(
                                post.getPostId()
                        ),
                        authenticatedUser.getUserId()
                )
        );

        boolean hasNext = response.hasNext();

        PostSuccessCode successCode = hasNext
                ? PostSuccessCode.POST_LIST_FOUND
                : PostSuccessCode.NO_MORE_POSTS;

        return ResponseEntity.ok(
                ApiResponse.success(
                        successCode,
                        SliceResponse.from(
                                response,
                                PostSuccessCode.NO_MORE_POSTS.getMessage()
                        )
                )
        );
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

        return ResponseEntity.ok(
                PostDetailResponse.from(
                        post,
                        comments,
                        postService.getPostImageUrl(post),
                        postLikeService.isLiked(
                                authenticatedUser.getUserId(),
                                postId
                        ),
                        bookmarkService.existsBookmark(
                                authenticatedUser.getUserId(),
                                postId
                        ),
                        authenticatedUser.getUserId()
                )
        );
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId
    ) {
        postService.deletePost(
                authenticatedUser.getUserId(),
                postId
        );

        return ResponseEntity.noContent().build();
    }

    // 게시글 좋아요
    @PostMapping("/{postId}/likes")
    public ResponseEntity<PostLikeResponse> toggleLike(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId
    ) {
        boolean liked = postLikeService.toggleLike(
                authenticatedUser.getUserId(),
                postId
        );
        int likeCount = Math.toIntExact(postLikeService.countLikes(postId));

        return ResponseEntity.ok(new PostLikeResponse(postId, liked, likeCount));
    }

    // 댓글 작성
    @PostMapping("/{postId}/comments/users/{userId}")
    public ResponseEntity<Long> createComment(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId,
            @Valid @RequestBody PostCommentCreateRequest request
    ) {
        Long commentId = commentService.createComment(
                authenticatedUser.getUserId(),
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody PostCommentUpdateRequest request
    ) {
        commentService.updateComment(
                authenticatedUser.getUserId(),
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
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId,
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(
                authenticatedUser.getUserId(),
                postId,
                commentId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/bookmarks")
    public ResponseEntity<ApiResponse<BookmarkResponse>> addBookmark(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId
    ) {
        boolean created = bookmarkService.addBookmark(
                authenticatedUser.getUserId(),
                postId
        );

        BookmarkSuccessCode successCode = created
                ? BookmarkSuccessCode.BOOKMARK_CREATED
                : BookmarkSuccessCode.BOOKMARK_ALREADY_SAVED;

        return ResponseEntity.status(successCode.getStatus()).body(
                ApiResponse.success(
                        successCode,
                        new BookmarkResponse(postId, true)
                )
        );
    }

    @DeleteMapping("/{postId}/bookmarks")
    public ResponseEntity<Void> deleteBookmark(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @PathVariable Long postId
    ) {
        bookmarkService.deleteBookmark(
                authenticatedUser.getUserId(),
                postId
        );

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bookmarks")
    public ResponseEntity<
            ApiResponse<SliceResponse<PostResponse>>
            > findBookmarks(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PageableDefault(size = 10)
            Pageable pageable
    ) {
        Slice<PostResponse> response = bookmarkService
                .getBookmarkPosts(
                        authenticatedUser.getUserId(),
                        limitPageSize(pageable)
                )
                .map(post -> new PostResponse(
                        post,
                        postService.getPostImageUrl(post),
                        true,
                        authenticatedUser.getUserId()
                ));

        boolean hasNext = response.hasNext();

        BookmarkSuccessCode successCode = hasNext
                ? BookmarkSuccessCode.BOOKMARK_LIST_FOUND
                : BookmarkSuccessCode.NO_MORE_BOOKMARKS;

        return ResponseEntity.ok(
                ApiResponse.success(
                        successCode,
                        SliceResponse.from(
                                response,
                                BookmarkSuccessCode.NO_MORE_BOOKMARKS.getMessage()
                        )
                )
        );
    }

    private Pageable limitPageSize(Pageable pageable) {
        return PageRequest.of(
                Math.max(pageable.getPageNumber(), 0),
                Math.min(Math.max(pageable.getPageSize(), 1), 10)
        );
    }
}
