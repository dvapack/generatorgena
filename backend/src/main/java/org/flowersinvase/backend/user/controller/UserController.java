package org.flowersinvase.backend.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.flowersinvase.backend.user.dto.LoginRequest;
import org.flowersinvase.backend.user.dto.LoginResponse;
import org.flowersinvase.backend.user.dto.RegisterUserRequest;
import org.flowersinvase.backend.user.dto.RegisterUserResponse;
import org.flowersinvase.backend.user.service.UserService;
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