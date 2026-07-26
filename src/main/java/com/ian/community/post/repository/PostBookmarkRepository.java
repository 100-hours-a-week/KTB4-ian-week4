package com.ian.community.post.repository;

import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostBookmark;
import com.ian.community.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {
    Optional<PostBookmark> findByAuthorUserAndAuthorPost(User authorUser, Post authorPost);

    Page<PostBookmark> findAllByAuthorUserAndAuthorPost_PostDeletedFalseOrderByCreatedAtDesc(
            User authorUser,
            Pageable pageable
    );
}
