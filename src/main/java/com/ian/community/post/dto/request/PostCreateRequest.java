package com.ian.community.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {
    @NotBlank(message = "본문을 입력해주세요.")
    private String content;

    private String imageUrl;
}
