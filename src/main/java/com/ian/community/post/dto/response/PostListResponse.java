package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostListResponse {
    @JsonProperty("post_id")
    private Long postId;

    private String nickname;

    @JsonProperty("profile_image")
    private String profileImage;

    @JsonProperty("like_count")
    private int likeCount;

    @JsonProperty("view_count")
    private int viewCount;

    @JsonProperty("comment_count")
    private int commentCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
