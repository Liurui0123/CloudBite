package com.cloudbite.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 40)
    private String nickname;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String role = "USER";

    private LocalDateTime createdAt = LocalDateTime.now();
}