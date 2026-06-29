package com.ian.community.post.repository;

import com.ian.community.post.domain.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long>  {
    Optional<Post> findByPostIdAndPostDeletedFalse(Long postId); // 삭제되지 않은 게시글 단건 조회

    Page<Post> findAllByPostDeletedFalse(Pageable pageable); // 삭제되지 않은 게시글 목록 조회

    Page<Post> findAllByAuthorUser_UserIdAndPostDeletedFalse(Long userId, Pageable pageable); // 특정 유저의 삭제되지 않은 게시글 목록 조회

    boolean existsByPostIdAndPostDeletedFalse(Long postId); // 삭제되지 않은 게시글 존재 여부 확인
}
