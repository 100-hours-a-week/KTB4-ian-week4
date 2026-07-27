package com.ian.community.post.repository;

import com.ian.community.post.domain.Bookmark;
import com.ian.community.post.domain.Post;
import com.ian.community.user.domain.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    Optional<Bookmark> findByUserAndPost(User user, Post post);

    Page<Bookmark> findAllByUserAndPost_PostDeletedFalseOrderByCreatedAtDesc(
            User user,
            Pageable pageable
    );
}
