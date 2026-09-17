package org.flowersinvase.backend.controller.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.dto.user.LoginRequest;
import org.flowersinvase.backend.dto.user.LoginResponse;
import org.flowersinvase.backend.dto.user.RegisterUserRequest;
import org.flowersinvase.backend.dto.user.RegisterUserResponse;
import org.flowersinvase.backend.service.user.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUserResponse register(
            @Valid @RequestBody RegisterUserRequest request
    ) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return userService.login(request);
    }
}