package com.ian.community.post.dto.response;

import com.ian.community.post.domain.Post;
import com.ian.community.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PostOwnershipResponseTest {

    @Test
    void 게시물_작성자와_인증_사용자가_같으면_owner가_true다() {
        Post post = createPost(7L);

        PostResponse listResponse = new PostResponse(
                post,
                null,
                false,
                7L
        );
        PostDetailResponse detailResponse = PostDetailResponse.from(
                post,
                List.of(),
                null,
                false,
                false,
                7L
        );

        assertThat(listResponse.isOwner()).isTrue();
        assertThat(detailResponse.isOwner()).isTrue();
    }

    @Test
    void 게시물_작성자와_인증_사용자가_다르면_owner가_false다() {
        Post post = createPost(7L);

        PostResponse listResponse = new PostResponse(
                post,
                null,
                false,
                8L
        );
        PostDetailResponse detailResponse = PostDetailResponse.from(
                post,
                List.of(),
                null,
                false,
                false,
                8L
        );

        assertThat(listResponse.isOwner()).isFalse();
        assertThat(detailResponse.isOwner()).isFalse();
    }

    private Post createPost(Long authorUserId) {
        User author = new User(
                "author@example.com",
                "encoded-password",
                "작성자"
        );
        ReflectionTestUtils.setField(
                author,
                "userId",
                authorUserId
        );

        return new Post(author, "게시물 내용");
    }
}
