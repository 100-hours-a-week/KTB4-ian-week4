package com.ian.community.post.domain;

import com.ian.community.user.domain.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post_bookmarks",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_post_bookmarks_user_post",
            columnNames = {"user_id", "post_id"}
        )
    }
)
public class PostBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bookmark_id")
    private Long bookmarkId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User authorUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post authorPost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostBookmark(User authorUser, Post authorPost) {
        this.authorUser = authorUser;
        this.authorPost = authorPost;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }
}
