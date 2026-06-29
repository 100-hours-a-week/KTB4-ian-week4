package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostViewResponse {
    @JsonProperty("post_id")
    private Long postId;

    @JsonProperty("view_count")
    private int viewCount;

    private boolean increased;
}
