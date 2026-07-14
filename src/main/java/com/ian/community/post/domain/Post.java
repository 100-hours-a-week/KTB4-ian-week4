package com.ian.community.post.domain;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import com.ian.community.user.domain.User;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User authorUser;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(nullable = false)
    private Boolean commentable;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "post_deleted", nullable = false)
    private boolean postDeleted;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Post(User authorUser, String content) {
        this.authorUser = authorUser;
        this.content = content;
        this.likeCount = 0;
        this.viewCount = 0;
        this.commentCount = 0;
        this.commentable = true; // 기본적으로 사용자가 선택하지 않을 경우에 기본값
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now(); // Null 처리를 하려고 했다가 null 허용하지 않고 안전하게 게시글 생성 시에도 시간 데이터 넣도록 했습니다.
        this.postDeleted = false;
        this.deletedAt = null;
    }

    public void update(String content) {
        this.content = content;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount--;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseCommentCount() {
        this.commentCount++;
    }

    public void decreaseCommentCount() {
        this.commentCount--;
    }

    public void delete() {
        this.postDeleted = true;
        this.deletedAt = LocalDateTime.now();
    }
}
