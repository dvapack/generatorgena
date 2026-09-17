package org.flowersinvase.backend.mapper;

import org.flowersinvase.backend.dto.user.RegisterUserRequest;
import org.flowersinvase.backend.dto.user.RegisterUserResponse;
import org.flowersinvase.backend.entity.user.User;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class UserMapper {

    public User toUser(UUID userId, String passwordHash, RegisterUserRequest request) {
        return new User(userId, request.email(), passwordHash);
    }

    public RegisterUserResponse toRegisterUserResponse(User user) {
        return new RegisterUserResponse(user.id(), user.email());
    }
}
