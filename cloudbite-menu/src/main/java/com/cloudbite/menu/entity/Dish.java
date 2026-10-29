package com.cloudbite.menu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "t_dish")
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    private Long categoryId;

    private Integer stock = 100;

    /** ON_SALE / OFF_SALE */
    @Column(length = 16)
    private String status = "ON_SALE";

    private Integer sales = 0;

    @Column(length = 20)
    private String emoji;

    @Column(length = 30)
    private String tag;

    @Column(length = 255)
    private String imageUrl;
}