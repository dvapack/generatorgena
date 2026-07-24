package org.flowersinvase.backend.user.service;

import org.flowersinvase.backend.user.dto.LoginRequest;
import org.flowersinvase.backend.user.dto.LoginResponse;
import org.flowersinvase.backend.user.dto.RegisterUserRequest;
import org.flowersinvase.backend.user.dto.RegisterUserResponse;

public interface UserService {
    RegisterUserResponse register(RegisterUserRequest registerUserRequest);

    LoginResponse login(LoginRequest loginRequest);
}
