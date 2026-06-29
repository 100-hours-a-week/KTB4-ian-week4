package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostCreateResponse {
    @JsonProperty("post_id")
    private Long postId;

    private String title;

    private String content;

    @JsonProperty("like_count")
    private int likeCount;

    @JsonProperty("view_count")
    private int viewCount;

    @JsonProperty("comment_count")
    private int commentCount;

    private Boolean commentable;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("post_deleted")
    private Boolean postDeleted;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
}