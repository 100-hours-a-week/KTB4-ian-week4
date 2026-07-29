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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional
    public boolean addBookmark(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        if (bookmarkRepository.existsByUserAndPost(user, post)) {
            return false;
        }

        bookmarkRepository.save(new Bookmark(user, post));
        return true;
    }

    @Transactional
    public boolean deleteBookmark(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);
        Optional<Bookmark> bookmark =
                bookmarkRepository.findByUserAndPost(user, post);

        bookmark.ifPresent(bookmarkRepository::delete);
        return bookmark.isPresent();
    }

    public boolean existsBookmark(Long userId, Long postId) {
        User user = getActiveUser(userId);
        Post post = getActivePost(postId);

        return bookmarkRepository.existsByUserAndPost(user, post);
    }

    public Set<Long> findBookmarkedPostIds(
            Long userId,
            Collection<Long> postIds
    ) {
        getActiveUser(userId);

        if (postIds.isEmpty()) {
            return Set.of();
        }

        return bookmarkRepository
                .findBookmarkedPostIds(userId, postIds)
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    public Slice<Post> getBookmarkPosts(
            Long userId,
            Pageable pageable
    ) {
        User user = getActiveUser(userId);

        return bookmarkRepository
                .findAllByUserAndPost_PostDeletedFalseOrderByCreatedAtDescBookmarkIdDesc(
                        user,
                        pageable
                )
                .map(Bookmark::getPost);
    }

    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        if (user.isUserDeleted()) {
            throw new CustomException(ErrorCode.USER_ALREADY_DELETED);
        }

        return user;
    }

    private Post getActivePost(Long postId) {
        return postRepository
                .findByPostIdAndPostDeletedFalse(postId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.POST_NOT_FOUND)
                );
    }
}
