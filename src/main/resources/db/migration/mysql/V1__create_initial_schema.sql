CREATE TABLE users (
    user_id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(256),
    password VARCHAR(100),
    nickname VARCHAR(10) NOT NULL,
    profile_image VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    password_updated_at DATETIME(6),
    nickname_updated_at DATETIME(6),
    profile_updated_at DATETIME(6),
    user_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE posts (
    post_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    like_count INT NOT NULL,
    view_count INT NOT NULL,
    comment_count INT NOT NULL,
    commentable BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    post_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (post_id),
    CONSTRAINT fk_posts_user
        FOREIGN KEY (user_id) REFERENCES users (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE post_comments (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    comment TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    comment_deleted BOOLEAN NOT NULL,
    deleted_at DATETIME(6),
    PRIMARY KEY (comment_id),
    CONSTRAINT fk_post_comments_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_post_comments_post
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE post_images (
    post_image_id BIGINT NOT NULL AUTO_INCREMENT,
    post_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    create_at DATETIME(6) NOT NULL,
    PRIMARY KEY (post_image_id),
    CONSTRAINT uk_post_images_post UNIQUE (post_id),
    CONSTRAINT fk_post_images_post
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE post_likes (
    likes_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (likes_id),
    CONSTRAINT uk_post_likes_user_post UNIQUE (user_id, post_id),
    CONSTRAINT fk_post_likes_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_post_likes_post
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE post_views (
    view_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    viewed_at DATETIME(6) NOT NULL,
    PRIMARY KEY (view_id),
    CONSTRAINT uk_post_views_user_post UNIQUE (user_id, post_id),
    CONSTRAINT fk_post_views_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_post_views_post
        FOREIGN KEY (post_id) REFERENCES posts (post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    refresh_token_id BIGINT NOT NULL AUTO_INCREMENT,
    token_id VARCHAR(36) NOT NULL,
    family_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    revoked BOOLEAN NOT NULL,
    replaced_by_token_id VARCHAR(36),
    PRIMARY KEY (refresh_token_id),
    CONSTRAINT uk_refresh_tokens_token_id UNIQUE (token_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE bookmarks (
    bookmark_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (bookmark_id),
    CONSTRAINT uk_bookmarks_user_post UNIQUE (user_id, post_id),
    CONSTRAINT fk_bookmarks_user
        FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_bookmarks_post
        FOREIGN KEY (post_id) REFERENCES posts (post_id),
    INDEX idx_bookmarks_user_created_at (user_id, created_at DESC, bookmark_id DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE token_family_sessions (
    family_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    active_access_token_id VARCHAR(36) NOT NULL,
    revoked BOOLEAN NOT NULL,
    PRIMARY KEY (family_id),
    INDEX idx_token_family_sessions_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
