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
@Table(name = "post_views",
    uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_post_views_user_post",
                columnNames = {"user_id", "post_id"}
        )
    }
)
public class PostView {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "view_id")
    private Long viewId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User authorUser;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post authorPost;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    public PostView(User authorUser, Post authorPost) {
        this.authorUser = authorUser;
        this.authorPost = authorPost;
        this.viewedAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public void updateViewedAt() {
        this.viewedAt = LocalDateTime.now();
    }
}
