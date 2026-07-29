package org.flowersinvase.backend.service.user;

import org.flowersinvase.backend.dto.user.LoginRequest;
import org.flowersinvase.backend.dto.user.LoginResponse;
import org.flowersinvase.backend.dto.user.RegisterUserRequest;
import org.flowersinvase.backend.dto.user.RegisterUserResponse;

public interface UserService {
    RegisterUserResponse register(RegisterUserRequest registerUserRequest);

    LoginResponse login(LoginRequest loginRequest);
}
