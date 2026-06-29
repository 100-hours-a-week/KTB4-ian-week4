package com.ian.community.post.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostUpdateResponse {
    private Long postId;
    private String title;
    private String content;
    private String imageUrl;
    private LocalDateTime updatedAt;
}
