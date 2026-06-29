package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostLikeResponse {
    @JsonProperty("post_id")
    private Long postId;

    private boolean liked;

    @JsonProperty("like_count")
    private int likeCount;
}
