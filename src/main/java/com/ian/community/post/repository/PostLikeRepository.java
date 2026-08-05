package com.ian.community.post.repository;

import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostLike;
import com.ian.community.user.domain.User;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Table(name = "post_likes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_likes_user_post",
                        columnNames = {"user_id", "post_id"}
                )
        }
)
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByAuthorUserAndAuthorPost(User authorUser, Post authorPost);

    long countByAuthorPost(Post authorPost);

    @Query("""
            select postLike.authorPost.postId
            from PostLike postLike
            where postLike.authorUser.userId = :userId
              and postLike.authorPost.postId in :postIds
              and postLike.authorPost.postDeleted = false
            """)
    List<Long> findLikedPostIds(
            @Param("userId") Long userId,
            @Param("postIds") Collection<Long> postIds
    );
}
