package com.ian.community.post.repository;

import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostView;
import com.ian.community.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, Long> {
    Optional<PostView> findByAuthorUserAndAuthorPost(User authorUser, Post authorPost);
}
