package com.ian.community.post.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class PostCreateRequest {
    @NotEmpty(message = "제목을 입력해주세요.")
    @Size(max = 26,message = "제목은 최대 26자까지 작성 가능합니다.")
    private String title;

    @NotEmpty(message = "본문을 입력해주세요.")
    private String content;

    private String imageUrl;
}
