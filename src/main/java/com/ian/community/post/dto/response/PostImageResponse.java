package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostImageResponse {
    @JsonProperty("post_image_id")
    private Long PostImageId;

    @JsonProperty("author_post")
    private Post authorPost;

    @JsonProperty("image_url")
    private String imageUrl;
}
