INSERT INTO users (
    email,
    password,
    nickname,
    profile_image,
    created_at,
    user_deleted
)
SELECT
    'email@email.com',
    '$2y$10$wGKo9RlfZVqaIkzjuJvFiuTJtdd.tcLOzM3YAMxaQSCSyUc40u/2u',
    '아아아',
    '/images/profile-default.svg',
    CURRENT_TIMESTAMP,
    FALSE
WHERE NOT EXISTS (
    SELECT 1
    FROM users
    WHERE email = 'email@email.com'
);
