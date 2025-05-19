package cqrs.user.command;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cqrs.user.model.User;
import cqrs.user.model.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 사용자 관련 Command(생성, 수정, 삭제)를 처리하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {
    private final UserRepository userRepository;

    @Transactional
    public User createUser(UserCreatedCommand command) {
        User user = new User(UUID.randomUUID(), command.name());
        userRepository.save(user);
        log.info("User created. userId={}, name={}", user.getUserId(), user.getName());
        return user;
    }
}