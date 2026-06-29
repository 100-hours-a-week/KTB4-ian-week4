package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostDeleteResponse {
    @JsonProperty("post_id")
    private Long postId;

    @JsonProperty("post_deleted")
    private Boolean postDeleted;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;
}
