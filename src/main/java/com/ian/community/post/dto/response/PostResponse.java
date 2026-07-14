package com.ian.community.post.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ian.community.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostResponse {
    @JsonProperty("post_id")
    private Long postId;

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

    @JsonProperty("image_url")
    private String imageUrl;

    public PostResponse(Post post) {
        this(post, null);
    }

    public PostResponse(Post post, String imageUrl) {
        this.postId = post.getPostId();
        this.content = post.getContent();
        this.authorName = post.getAuthorUser().getNickname();
        this.profileImage = post.getAuthorUser().getProfileImage();
        this.likeCount = post.getLikeCount();
        this.viewCount = post.getViewCount();
        this.commentCount = post.getCommentCount();
        this.createdAt = post.getCreatedAt();
        this.updatedAt = post.getUpdatedAt();
        this.postDeleted = post.isPostDeleted();
        this.imageUrl = imageUrl;
    }
}
