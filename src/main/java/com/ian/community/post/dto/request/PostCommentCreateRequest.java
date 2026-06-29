package com.ian.community.post.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCommentCreateRequest {
    @NotBlank(message = "댓글을 작성해주세요.")
    @Size(min = 1, message = "댓글은 1글자 이상 작성해야 합니다.")
    private String comment;
}
