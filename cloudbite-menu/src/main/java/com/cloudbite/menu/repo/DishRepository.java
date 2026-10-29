package com.cloudbite.menu.repo;

import com.cloudbite.menu.entity.Dish;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DishRepository extends JpaRepository<Dish, Long> {
    List<Dish> findByStatusOrderBySalesDesc(String status);

    List<Dish> findByCategoryIdAndStatusOrderBySalesDesc(Long categoryId, String status);

    List<Dish> findByNameContainingAndStatus(String keyword, String status);

    long countByCategoryId(Long categoryId);
}