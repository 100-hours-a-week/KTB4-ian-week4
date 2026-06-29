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
@Table(name = "post_likes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_post_likes_user_post",
            columnNames = {"user_id", "post_id"}
        )
    }
)
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "likes_id")
    private Long likeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User authorUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post authorPost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostLike(User authorUser, Post authorPost) {
        this.authorUser = authorUser;
        this.authorPost = authorPost;
        this.createdAt = LocalDateTime.now();
    }
}
