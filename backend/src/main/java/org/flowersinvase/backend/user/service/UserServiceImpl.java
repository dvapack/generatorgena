package org.flowersinvase.backend.user.service;

import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.security.TokenService;
import org.flowersinvase.backend.user.dto.LoginRequest;
import org.flowersinvase.backend.user.dto.LoginResponse;
import org.flowersinvase.backend.user.dto.RegisterUserRequest;
import org.flowersinvase.backend.user.dto.RegisterUserResponse;
import org.flowersinvase.backend.user.entity.User;
import org.flowersinvase.backend.user.exception.EmailAlreadyExistsException;
import org.flowersinvase.backend.user.exception.InvalidCredentialsException;
import org.flowersinvase.backend.user.repository.UserRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String EMAIL_EXISTS_MESSAGE = "Пользователь с таким email уже существует";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    @Override
    @Transactional
    public RegisterUserResponse register(RegisterUserRequest request) {
        String email = request.email();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(EMAIL_EXISTS_MESSAGE);
        }
        User user = new User(
                UUID.ofEpochMillis(System.currentTimeMillis()),
                email,
                passwordEncoder.encode(request.password())
        );
        try {
            User savedUser = userRepository.save(user);

            return new RegisterUserResponse(
                    savedUser.id(),
                    savedUser.email()
            );
        } catch (DuplicateKeyException exception) {
            // чтобы параллельные запросы не могли зарегать один и тот же email
            throw new EmailAlreadyExistsException(EMAIL_EXISTS_MESSAGE);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Неверный email или пароль"));
        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException("Неверный email или пароль");
        }
        return tokenService.createAccessToken(user);
    }
}