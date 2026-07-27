package com.ian.community.post.service;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.post.domain.Bookmark;
import com.ian.community.post.domain.Post;
import com.ian.community.post.repository.BookmarkRepository;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public boolean toggleBookmark(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        Optional<Bookmark> bookmarkOptional =
                bookmarkRepository.findByUserAndPost(user, post);

        if (bookmarkOptional.isPresent()) {
            bookmarkRepository.delete(bookmarkOptional.get());
            return false;
        }

        try {
            bookmarkRepository.save(new Bookmark(user, post));
            return true;
        } catch (DataIntegrityViolationException exception) {
            throw new CustomException(ErrorCode.BOOKMARK_ALREADY_EXISTS);
        } catch (RuntimeException exception) {
            throw new CustomException(ErrorCode.BOOKMARK_OPERATION_FAILED);
        }
    }

    public boolean existsBookmark(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        return bookmarkRepository.findByUserAndPost(user, post).isPresent();
    }

    public Slice<Post> getBookmarkPosts(Long userId, Pageable pageable) {
        User user = getActiveUser(userId);

        return bookmarkRepository
                .findAllByUserAndPost_PostDeletedFalseOrderByCreatedAtDesc(
                        user,
                        pageable
                )
                .map(Bookmark::getPost);
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.isUserDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        return user;
    }

    private Post getActivePost(Long postId) {
        return postRepository.findByPostIdAndPostDeletedFalse(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
    }
}
