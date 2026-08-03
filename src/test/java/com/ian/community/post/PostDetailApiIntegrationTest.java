package com.ian.community.post;

import com.ian.community.post.domain.Post;
import com.ian.community.post.domain.PostComment;
import com.ian.community.post.repository.CommentRepository;
import com.ian.community.post.repository.PostRepository;
import com.ian.community.security.jwt.JwtCookieProvider;
import com.ian.community.security.token.TokenService;
import com.ian.community.user.domain.User;
import com.ian.community.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostDetailApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TokenService tokenService;

    @Test
    @DisplayName("인증 사용자는 me 경로로 게시글을 작성한다")
    void createPostThroughAuthenticatedUserRoute() throws Exception {
        User author = saveUser(
                "post-me-author@example.com",
                "인증글작성자"
        );

        mockMvc.perform(
                        post("/api/posts/me")
                                .with(csrf())
                                .cookie(accessCookie(author))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "content": "인증 사용자 게시글"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("다른 사용자의 댓글이 있는 게시글 상세를 조회한다")
    void getDetailWithAnotherUsersComment() throws Exception {
        User author = saveUser(
                "detail-author@example.com",
                "상세작성자"
        );
        User viewer = saveUser(
                "detail-viewer@example.com",
                "상세조회자"
        );
        User commentAuthor = saveUser(
                "detail-commenter@example.com",
                "기존댓글작성자"
        );
        Post post = postRepository.saveAndFlush(
                new Post(author, "댓글이 있는 게시글")
        );
        PostComment comment = commentRepository.saveAndFlush(
                new PostComment(
                        commentAuthor,
                        post,
                        "기존 댓글"
                )
        );

        mockMvc.perform(
                        get("/api/posts/{postId}", post.getPostId())
                                .cookie(accessCookie(viewer))
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.post_id")
                                .value(post.getPostId())
                )
                .andExpect(
                        jsonPath("$.comment[0].comment_id")
                                .value(comment.getCommentId())
                )
                .andExpect(
                        jsonPath("$.comment[0].user_id")
                                .value(commentAuthor.getUserId())
                )
                .andExpect(
                        jsonPath("$.comment[0].nickname")
                                .value(commentAuthor.getNickname())
                );
    }

    @Test
    @DisplayName("다른 사용자의 게시글에 댓글을 작성한 뒤 상세를 다시 조회한다")
    void reloadDetailAfterCreatingComment() throws Exception {
        User author = saveUser(
                "comment-author@example.com",
                "댓글글작성자"
        );
        User commenter = saveUser(
                "comment-writer@example.com",
                "댓글작성자"
        );
        Post post = postRepository.saveAndFlush(
                new Post(author, "댓글 작성 대상")
        );
        Cookie accessCookie = accessCookie(commenter);

        mockMvc.perform(
                        post(
                                "/api/posts/{postId}/comments/users/me",
                                post.getPostId()
                        )
                                .with(csrf())
                                .cookie(accessCookie)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "comment": "새 댓글"
                                        }
                                        """)
                )
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/posts/{postId}", post.getPostId())
                                .cookie(accessCookie)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.comment_count")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.comment[0].user_id")
                                .value(commenter.getUserId())
                )
                .andExpect(
                        jsonPath("$.comment[0].nickname")
                                .value(commenter.getNickname())
                )
                .andExpect(
                        jsonPath("$.comment[0].comment")
                                .value("새 댓글")
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
                tokenService.issueInitialTokens(user).accessToken()
        );
    }
}
