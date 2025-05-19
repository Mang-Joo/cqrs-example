package cqrs.user.query;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cqrs.user.model.User;
import cqrs.user.model.UserRepository;
import lombok.RequiredArgsConstructor;

/**
 * 사용자 관련 Query(조회)를 처리하는 서비스입니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserQueryService {

    private final UserRepository userRepository;

    public User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }
}