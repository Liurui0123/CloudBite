package com.cloudbite.ai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "t_ai_log")
public class AiLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(length = 20)
    private String type;

    @Column(length = 500)
    private String prompt;

    @Column(length = 4000)
    private String answer;

    private LocalDateTime createdAt = LocalDateTime.now();
}