package com.ian.community.post.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "post_images",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_post_images_post",
            columnNames = "post_id"
        )
    }
)
public class PostImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long PostImageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post authorPost;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "create_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PostImage(Post authorPost, String imageUrl) {
        this.authorPost = authorPost;
        this.imageUrl = imageUrl;
        this.createdAt = LocalDateTime.now();
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
