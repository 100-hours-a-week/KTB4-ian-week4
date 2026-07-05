package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.PostComment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostCommentResponse {
    @JsonProperty("comment_id")
    private Long commentId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("post_id")
    private Long postId;

    private String comment;

    private String nickname;

    @JsonProperty("profile_image")
    private String profileImage;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    public static PostCommentResponse from(PostComment comment) {
        return new PostCommentResponse(
                comment.getCommentId(),
                comment.getAuthorUser().getUserId(),
                comment.getAuthorPost().getPostId(),
                comment.getComment(),
                comment.getAuthorUser().getNickname(),
                comment.getAuthorUser().getProfileImage(),
                comment.getCreatedAt()
        );
    }
}
