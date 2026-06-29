package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostResponse { // 삭제 예정
    @JsonProperty("post_id")
    private Long postId;

    private String title;

    private String content;

    @JsonProperty("author_name")
    private String authorName;

    @JsonProperty("profile_image")
    private String profileImage;

    @JsonProperty("like_count")
    private int likeCount;

    @JsonProperty("comment_count")
    private int commentCount;

    @JsonProperty("view_count")
    private int viewCount;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("post_deleted")
    private boolean postDeleted;

    public PostResponse(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
    }
}
