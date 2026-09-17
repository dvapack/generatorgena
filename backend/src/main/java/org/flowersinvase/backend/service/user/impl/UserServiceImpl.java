package org.flowersinvase.backend.service.user.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowersinvase.backend.mapper.UserMapper;
import org.flowersinvase.backend.security.TokenService;
import org.flowersinvase.backend.dto.user.LoginRequest;
import org.flowersinvase.backend.dto.user.LoginResponse;
import org.flowersinvase.backend.dto.user.RegisterUserRequest;
import org.flowersinvase.backend.dto.user.RegisterUserResponse;
import org.flowersinvase.backend.entity.user.User;
import org.flowersinvase.backend.exception.user.EmailAlreadyExistsException;
import org.flowersinvase.backend.exception.user.InvalidCredentialsException;
import org.flowersinvase.backend.repository.user.UserRepository;
import org.flowersinvase.backend.service.user.UserService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String EMAIL_EXISTS_MESSAGE = "Пользователь с таким email уже существует";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        String email = request.email();
        if (userRepository.existsByEmail(email)) {
            log.debug("Ошибка регистрации: попытка регистрации пользователя с существующим email");
            throw new EmailAlreadyExistsException(EMAIL_EXISTS_MESSAGE);
        }
        User user = userMapper.toUser(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                passwordEncoder.encode(request.password()),
                request
        );
        try {
            User savedUser = userRepository.save(user);
            log.info("Пользователь зарегистрирован: id={}", savedUser.id());
            return userMapper.toRegisterUserResponse(savedUser);
        } catch (DuplicateKeyException exception) {
            log.debug("Ошибка регистрации: попытка регистрации пользователя с существующим email");
            throw new EmailAlreadyExistsException(EMAIL_EXISTS_MESSAGE);
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    log.debug("Ошибка входа: пользователь не найден");
                    return new InvalidCredentialsException("Неверный email или пароль");
                });
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            log.debug("Ошибка входа: неправильный пароль у пользователя id={}", user.id());
            throw new InvalidCredentialsException("Неверный email или пароль");
        }
        log.debug("Пользователь успешно вошёл: id={}", user.id());
        return tokenService.createAccessToken(user);
    }
}