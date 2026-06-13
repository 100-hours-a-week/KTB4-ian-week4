package com.ian.community.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostResponse {
    @JsonProperty("post_id")
    private Long postId;

    private String title;

    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("profile_image")
    private String profileImage;

    @JsonProperty("like_count")
    private int likeCount;

    @JsonProperty("comment_count")
    private int commentCount;

    @JsonProperty("view_count")
    private int viewCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("post_deleted")
    private boolean postDeleted;

    public PostResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.authorName = post.getAuthorName();
        this.profileImage = post.getProfileImage();
        this.likeCount = post.getLikeCount();
        this.commentCount = post.getCommentCount();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.postDeleted = post.isPostDeleted();
    }
}
