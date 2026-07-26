package com.ian.community.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostBookmarkResponse {
    private Long postId;
    private boolean bookmarked;
}
