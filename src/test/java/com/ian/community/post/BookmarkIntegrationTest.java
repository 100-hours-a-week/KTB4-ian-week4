package com.ian.community.post;

import com.ian.community.common.exception.CustomException;
import com.ian.community.common.exception.ErrorCode;
import com.ian.community.post.domain.Post;
import com.ian.community.post.repository.BookmarkRepository;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.post.service.BookmarkService;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class BookmarkIntegrationTest {

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.saveAndFlush(
                new User(
                        "bookmark-user@example.com",
                        "encoded-password",
                        "북마크사용자"
                )
        );
    }

    @Test
    @DisplayName("Bookmark 추가는 멱등이고 삭제는 존재하지 않아도 안전하다")
    void addAndDeleteIdempotently() {
        Post post = savePost(user, "북마크 대상");

        assertThat(
                bookmarkService.addBookmark(
                        user.getUserId(),
                        post.getPostId()
                )
        ).isTrue();
        assertThat(
                bookmarkService.addBookmark(
                        user.getUserId(),
                        post.getPostId()
                )
        ).isFalse();
        assertThat(bookmarkRepository.count()).isOne();
        assertThat(
                bookmarkService.deleteBookmark(
                        user.getUserId(),
                        post.getPostId()
                )
        ).isTrue();
        assertThat(
                bookmarkService.deleteBookmark(
                        user.getUserId(),
                        post.getPostId()
                )
        ).isFalse();
    }

    @Test
    @DisplayName("사용자별 Bookmark 변경은 서로 격리된다")
    void isolateBookmarksByUser() {
        User other = userRepository.saveAndFlush(
                new User(
                        "bookmark-other@example.com",
                        "encoded-password",
                        "다른사용자"
                )
        );
        Post post = savePost(user, "공통 게시글");

        bookmarkService.addBookmark(user.getUserId(), post.getPostId());
        bookmarkService.addBookmark(other.getUserId(), post.getPostId());
        bookmarkService.deleteBookmark(user.getUserId(), post.getPostId());

        assertThat(
                bookmarkService.existsBookmark(
                        user.getUserId(),
                        post.getPostId()
                )
        ).isFalse();
        assertThat(
                bookmarkService.existsBookmark(
                        other.getUserId(),
                        post.getPostId()
                )
        ).isTrue();
    }

    @Test
    @DisplayName("삭제 게시글은 Bookmark할 수 없다")
    void rejectDeletedPost() {
        Post post = savePost(user, "삭제 게시글");
        post.delete();
        postRepository.flush();

        assertThatThrownBy(() ->
                bookmarkService.addBookmark(
                        user.getUserId(),
                        post.getPostId()
                )
        )
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.POST_NOT_FOUND);
    }

    @Test
    @DisplayName("Bookmark Slice는 10개, hasNext, 마지막 Slice와 중복 없음 계약을 지킨다")
    void bookmarkSliceContract() {
        for (int index = 0; index < 11; index++) {
            Post post = savePost(user, "게시글 " + index);
            bookmarkService.addBookmark(
                    user.getUserId(),
                    post.getPostId()
            );
        }

        Slice<Post> first = bookmarkService.getBookmarkPosts(
                user.getUserId(),
                PageRequest.of(0, 10)
        );
        Slice<Post> last = bookmarkService.getBookmarkPosts(
                user.getUserId(),
                PageRequest.of(1, 10)
        );

        assertThat(first.getContent()).hasSize(10);
        assertThat(first.hasNext()).isTrue();
        assertThat(last.getContent()).hasSize(1);
        assertThat(last.hasNext()).isFalse();

        List<Long> allIds = java.util.stream.Stream
                .concat(
                        first.stream().map(Post::getPostId),
                        last.stream().map(Post::getPostId)
                )
                .toList();
        assertThat(new HashSet<>(allIds))
                .hasSize(11);
    }

    private Post savePost(User author, String content) {
        return postRepository.saveAndFlush(
                new Post(author, content)
        );
    }
}
