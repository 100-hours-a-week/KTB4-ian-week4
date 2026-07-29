package com.ian.community.post;

import com.ian.community.post.domain.Post;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.post.service.BookmarkService;
import com.ian.community.security.jwt.JwtCookieProvider;
import com.ian.community.security.jwt.JwtTokenProvider;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
                jwtTokenProvider.createAccessToken(
                        user.getUserId(),
                        user.getEmail(),
                        List.of("USER")
                )
        );
    }
}