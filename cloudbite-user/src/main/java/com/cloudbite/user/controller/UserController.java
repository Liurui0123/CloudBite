package com.cloudbite.user.controller;

import com.cloudbite.common.ApiResult;
import com.cloudbite.common.AuthHeaders;
import com.cloudbite.user.dto.LoginRequest;
import com.cloudbite.user.dto.LoginResponse;
import com.cloudbite.user.dto.RegisterRequest;
import com.cloudbite.user.entity.User;
import com.cloudbite.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ApiResult<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResult.ok(service.register(req));
    }

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResult.ok(service.login(req));
    }

    @GetMapping("/me")
    public ApiResult<User> me(@RequestHeader(AuthHeaders.USER_ID) Long userId) {
        return ApiResult.ok(service.me(userId));
    }

    @GetMapping("/list")
    public ApiResult<List<User>> list() {
        return ApiResult.ok(service.list());
    }
}