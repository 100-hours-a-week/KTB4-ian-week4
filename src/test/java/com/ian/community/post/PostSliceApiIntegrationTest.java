package com.ian.community.post;

import com.ian.community.post.domain.Post;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.post.service.BookmarkService;
import com.ian.community.security.jwt.JwtCookieProvider;
import com.ian.community.security.jwt.JwtTokenProvider;
import com.ian.community.security.token.TokenService;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PostSliceApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("마지막 피드 Slice는 종료 정보를 응답한다")
    void feedLastSliceReturnsTerminalMessage()
            throws Exception {

        User user = saveUser(
                "feed-slice@example.com",
                "feedSlice"
        );

        postRepository.save(
                new Post(user, "마지막 피드")
        );

        mockMvc.perform(
                        get("/api/posts")
                                .param("page", "0")
                                .param("size", "10")
                                .cookie(accessCookie(user))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("NO_MORE_POSTS")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "더 이상 조회할 피드가 없습니다."
                                )
                )
                .andExpect(
                        jsonPath("$.data.hasNext")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.data.message")
                                .value(
                                        "더 이상 조회할 피드가 없습니다."
                                )
                );
    }

    @Test
    @DisplayName("마지막 북마크 Slice는 종료 정보를 응답한다")
    void bookmarkLastSliceReturnsTerminalMessage()
            throws Exception {

        User user = saveUser(
                "bookmark-slice@example.com",
                "bookSlice"
        );

        Post post = postRepository.save(
                new Post(user, "마지막 북마크")
        );

        bookmarkService.addBookmark(
                user.getUserId(),
                post.getPostId()
        );

        mockMvc.perform(
                        get("/api/posts/bookmarks")
                                .param("page", "0")
                                .param("size", "10")
                                .cookie(accessCookie(user))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.code")
                                .value("NO_MORE_BOOKMARKS")
                )
                .andExpect(
                        jsonPath("$.data.hasNext")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.data.message")
                                .value(
                                        "더 이상 조회할 북마크가 없습니다."
                                )
                );
    }

    @Test
    @DisplayName("피드 요청 크기는 10으로 제한하고 작성자 ID와 다음 Slice를 반환한다")
    void limitFeedSliceToTen() throws Exception {
        User user = saveUser("feed-limit@example.com", "feedLimit");
        List<Post> savedPosts = new java.util.ArrayList<>();
        for (int index = 0; index < 11; index++) {
            savedPosts.add(postRepository.save(new Post(user, "피드 " + index)));
        }
        LocalDateTime sameCreatedAt = LocalDateTime.of(2026, 7, 29, 10, 0);
        org.springframework.test.util.ReflectionTestUtils.setField(
                savedPosts.get(0),
                "createdAt",
                sameCreatedAt
        );
        org.springframework.test.util.ReflectionTestUtils.setField(
                savedPosts.get(1),
                "createdAt",
                sameCreatedAt
        );
        for (int index = 2; index < savedPosts.size(); index++) {
            org.springframework.test.util.ReflectionTestUtils.setField(
                    savedPosts.get(index),
                    "createdAt",
                    sameCreatedAt.minusMinutes(index)
            );
        }
        postRepository.flush();

        mockMvc.perform(
                        get("/api/posts")
                                .param("page", "0")
                                .param("size", "100")
                                .cookie(accessCookie(user))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("POST_LIST_FOUND"))
                .andExpect(jsonPath("$.data.content.length()").value(10))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.content[0].user_id")
                        .value(user.getUserId()))
                .andExpect(jsonPath("$.data.content[0].post_id")
                        .value(savedPosts.get(1).getPostId()))
                .andExpect(jsonPath("$.data.content[1].post_id")
                        .value(savedPosts.get(0).getPostId()));
    }

    @Test
    @DisplayName("피드 목록은 로그인 사용자별 좋아요 여부와 공통 좋아요 수를 반환한다")
    void returnViewerSpecificLikedStateInFeedSlice() throws Exception {
        User author = saveUser("liked-author@example.com", "likeAuthor");
        User viewer = saveUser("liked-viewer@example.com", "likeViewer");
        Post post = postRepository.saveAndFlush(
                new Post(author, "좋아요 상태 피드")
        );
        Cookie authorAccess = accessCookie(author);

        mockMvc.perform(
                        post("/api/posts/{postId}/likes", post.getPostId())
                                .with(csrf())
                                .cookie(authorAccess)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.liked").value(true))
                .andExpect(jsonPath("$.like_count").value(1));

        mockMvc.perform(
                        get("/api/posts")
                                .param("page", "0")
                                .param("size", "10")
                                .cookie(authorAccess)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].liked").value(true))
                .andExpect(jsonPath("$.data.content[0].like_count").value(1));

        mockMvc.perform(
                        post("/api/posts/{postId}/bookmarks", post.getPostId())
                                .with(csrf())
                                .cookie(authorAccess)
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        get("/api/posts/bookmarks")
                                .param("page", "0")
                                .param("size", "10")
                                .cookie(authorAccess)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].liked").value(true))
                .andExpect(jsonPath("$.data.content[0].like_count").value(1));

        mockMvc.perform(
                        get("/api/posts")
                                .param("page", "0")
                                .param("size", "10")
                                .cookie(accessCookie(viewer))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].liked").value(false))
                .andExpect(jsonPath("$.data.content[0].like_count").value(1));
    }

    @Test
    @DisplayName("북마크 저장은 최초와 멱등 재요청의 성공 코드를 구분한다")
    void distinguishBookmarkCreatedAndAlreadySaved() throws Exception {
        User user = saveUser("bookmark-code@example.com", "북마크코드");
        Post post = postRepository.save(new Post(user, "북마크 코드"));
        Cookie access = accessCookie(user);

        mockMvc.perform(
                        post("/api/posts/{postId}/bookmarks", post.getPostId())
                                .with(csrf())
                                .cookie(access)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("BOOKMARK_CREATED"))
                .andExpect(jsonPath("$.data.bookmarked").value(true));

        mockMvc.perform(
                        post("/api/posts/{postId}/bookmarks", post.getPostId())
                                .with(csrf())
                                .cookie(access)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code")
                        .value("BOOKMARK_ALREADY_SAVED"))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    private User saveUser(
            String email,
            String nickname
    ) {
        return userRepository.saveAndFlush(
                new User(
                        email,
                        "encoded-password",
                        nickname
                )
        );
    }

    private Cookie accessCookie(User user) {
        return new Cookie(
                JwtCookieProvider.ACCESS_TOKEN_COOKIE,
                tokenService.issueInitialTokens(user).accessToken()
        );
    }
}
