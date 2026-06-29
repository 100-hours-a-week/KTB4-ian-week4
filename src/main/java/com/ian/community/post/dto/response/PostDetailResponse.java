package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PostDetailResponse {
    @JsonProperty("post_id")
    private Long postId;

    private String title;

    private String content;

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

    private PostDetailResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
        this.createdAt = post.getCreatedAt();
    }

    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(post);
    }
}
