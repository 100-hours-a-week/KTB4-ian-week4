package com.ian.community.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostListResponse {
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

    public PostListResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.authorName = post.getAuthorName();
        this.profileImage = post.getProfileImage();
        this.likeCount = post.getLikeCount();
        this.commentCount = post.getCommentCount();
        this.viewCount = post.getViewCount();
        this.createdAt = post.getCreatedAt();
    }
}
