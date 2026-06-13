package com.ian.community.post.domain;

import lombok.Getter;

import java.time.LocalDateTime;
@Getter
public class Post {
    private Long postId;

    private String title;
    private String content;
    private String authorName;
    private String profileImage;
    private String imageUrl;

    private int likeCount;
    private int commentCount;
    private int viewCount;

    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean postDeleted;

    public Post(Long postId, String title, String content, String authorName, String profileImage, String imageUrl) {
        this.postId = postId;
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.profileImage = profileImage;
        this.imageUrl = imageUrl;
        this.likeCount = 0;
        this.commentCount = 0;
        this.viewCount = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = null;
        this.postDeleted = false;
    }

    public void update(String title, String content, String imageUrl) {
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void downLikeCount() {
        this.likeCount--;
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void downCommentCount() {
        this.commentCount--;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void delete() {
        this.authorName = "알 수 없음";
        this.profileImage = "https://image.kr/default-profile.jpg";
        this.postDeleted = true;
    }
}
