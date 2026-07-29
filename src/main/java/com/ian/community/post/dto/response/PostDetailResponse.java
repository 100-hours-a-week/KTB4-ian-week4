package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Getter
@AllArgsConstructor
public class PostDetailResponse {
    @JsonProperty("post_id")
    private Long postId;

    private String content;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("image_url")
    private String imageUrl;

    private String nickname;

    @JsonProperty("profile_image")
    private String profileImage;

    @JsonProperty("like_count")
    private int likeCount;

    @JsonProperty("comment_count")
    private int commentCount;

    @JsonProperty("view_count")
    private int viewCount;

    private List<PostCommentResponse> comment;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("post_deleted")
    private boolean postDeleted;

    private boolean liked;

    private boolean bookmarked;

    private boolean owner;

    private PostDetailResponse(Post post) {
        this.postId = post.getPostId();
        this.content = post.getContent();
        this.userId = post.getAuthorUser().getUserId();
        this.nickname = post.getAuthorUser().getNickname();
        this.profileImage = post.getAuthorUser().getProfileImage();
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
        this.createdAt = post.getCreatedAt();
        this.postDeleted = post.isPostDeleted();
    }

    public static PostDetailResponse from(
            Post post,
            List<PostCommentResponse> comments,
            String imageUrl,
            boolean liked,
            boolean bookmarked
    ) {
        return from(
                post,
                comments,
                imageUrl,
                liked,
                bookmarked,
                null
        );
    }

    public static PostDetailResponse from(
            Post post,
            List<PostCommentResponse> comments,
            String imageUrl,
            boolean liked,
            boolean bookmarked,
            Long authenticatedUserId
    ) {
        PostDetailResponse response = new PostDetailResponse(post);
        response.comment = comments;
        response.imageUrl = imageUrl;
        response.liked = liked;
        response.bookmarked = bookmarked;
        response.owner = Objects.equals(
                post.getAuthorUser().getUserId(),
                authenticatedUserId
        );

        return response;
    }
}
