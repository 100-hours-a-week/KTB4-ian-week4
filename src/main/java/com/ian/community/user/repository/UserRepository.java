package com.ian.community.user.repository;

import com.ian.community.user.domain.User;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
public class UserRepository {
    private final Map<Long, User> users = new HashMap<>();
    private final Map<String, User> usersByEmail = new HashMap<>();
    private final Map<String, User> usersByNickname = new HashMap<>();
    private Long sequence = 1L;

    public User save(String email, String password, String nickname, String profile) {
        User user = new User(sequence, email, password, nickname, profile);
        users.put(sequence, user);
        usersByEmail.put(email, user);
        usersByNickname.put(nickname, user);
        sequence++;
        return user;
    }

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email));
    }

    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email);
    }

    public boolean existsByNickname(String nickname) {
        return usersByNickname.containsKey(nickname);
    }
}
