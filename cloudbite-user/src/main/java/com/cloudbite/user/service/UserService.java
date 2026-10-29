package com.cloudbite.user.service;

import com.cloudbite.common.BizException;
import com.cloudbite.common.JwtUtil;
import com.cloudbite.user.dto.LoginRequest;
import com.cloudbite.user.dto.LoginResponse;
import com.cloudbite.user.dto.RegisterRequest;
import com.cloudbite.user.entity.User;
import com.cloudbite.user.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {
    private final UserRepository repo;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo, JwtUtil jwtUtil) {
        this.repo = repo;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        repo.findByUsername(req.username()).ifPresent(u -> {
            throw new BizException("用户名已被占用");
        });
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(encoder.encode(req.password()));
        u.setNickname(req.nickname() == null || req.nickname().isBlank() ? req.username() : req.nickname());
        u.setPhone(req.phone());
        u.setRole("USER");
        repo.save(u);
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
        return new LoginResponse(token, u.getId(), u.getUsername(), u.getNickname(), u.getRole());
    }

    public LoginResponse login(LoginRequest req) {
        User u = repo.findByUsername(req.username())
                .orElseThrow(() -> new BizException(401, "用户名或密码错误"));
        if (!encoder.matches(req.password(), u.getPassword())) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole());
        return new LoginResponse(token, u.getId(), u.getUsername(), u.getNickname(), u.getRole());
    }

    public User me(Long id) {
        return repo.findById(id).orElseThrow(() -> new BizException(404, "用户不存在"));
    }

    public List<User> list() {
        return repo.findAll();
    }

    @Transactional
    public void seed() {
        if (repo.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("admin123"));
            admin.setNickname("餐厅管理员");
            admin.setRole("ADMIN");
            repo.save(admin);
        }
        if (repo.findByUsername("xiaoming").isEmpty()) {
            User u = new User();
            u.setUsername("xiaoming");
            u.setPassword(encoder.encode("123456"));
            u.setNickname("小明");
            u.setPhone("13800000001");
            u.setRole("USER");
            repo.save(u);
        }
    }
}