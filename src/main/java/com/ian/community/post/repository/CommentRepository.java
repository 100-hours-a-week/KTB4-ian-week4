package com.ian.community.post.repository;

import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostComment;
import com.ian.community.user.domain.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<PostComment, Long> {
    Slice<PostComment> findAllByAuthorPostAndCommentDeletedFalse(Post authorPost, Pageable pageable);

    Optional<PostComment> findByCommentIdAndCommentDeletedFalse(Long commentId);

    Optional<PostComment> findByAuthorPostAndCommentIdAndCommentDeletedFalse(Post authorPost, Long commentId);

    Optional<PostComment> findByAuthorUserAndAuthorPostAndCommentIdAndCommentDeletedFalse(User authorUser, Post authorPost, Long CommentId);
}