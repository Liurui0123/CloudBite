package com.cloudbite.menu.controller;

import com.cloudbite.common.ApiResult;
import com.cloudbite.menu.dto.DishRequest;
import com.cloudbite.menu.dto.StockItem;
import com.cloudbite.menu.entity.Category;
import com.cloudbite.menu.entity.Dish;
import com.cloudbite.menu.service.MenuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu")
public class MenuController {
    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    @GetMapping("/public/categories")
    public ApiResult<List<Category>> categories() {
        return ApiResult.ok(service.categories());
    }

    @GetMapping("/public/dishes")
    public ApiResult<List<Dish>> dishes(@RequestParam(required = false) Long categoryId,
                                        @RequestParam(required = false) String keyword) {
        return ApiResult.ok(service.dishes(categoryId, keyword));
    }

    @GetMapping("/public/hot")
    public ApiResult<List<java.util.Map<String, Object>>> hot(@RequestParam(defaultValue = "5") int top) {
        return ApiResult.ok(service.hot(top));
    }

    @GetMapping("/public/dishes/{id}")
    public ApiResult<Dish> dish(@PathVariable Long id) {
        return ApiResult.ok(service.dish(id));
    }

    @GetMapping("/dishes")
    public ApiResult<List<Dish>> allDishes() {
        return ApiResult.ok(service.allDishes());
    }

    @PostMapping("/dishes")
    public ApiResult<Dish> create(@RequestBody DishRequest req) {
        return ApiResult.ok(service.create(req));
    }

    @PutMapping("/dishes/{id}")
    public ApiResult<Dish> update(@PathVariable Long id, @RequestBody DishRequest req) {
        return ApiResult.ok(service.update(id, req));
    }

    @DeleteMapping("/dishes/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ApiResult.ok();
    }

    @PatchMapping("/dishes/{id}/toggle")
    public ApiResult<Void> toggle(@PathVariable Long id) {
        service.toggle(id);
        return ApiResult.ok();
    }

    @GetMapping("/categories")
    public ApiResult<List<Category>> adminCategories() {
        return ApiResult.ok(service.categories());
    }

    @PostMapping("/categories")
    public ApiResult<Category> createCategory(@RequestBody Category category) {
        return ApiResult.ok(service.createCategory(category));
    }

    @PutMapping("/categories/{id}")
    public ApiResult<Category> updateCategory(@PathVariable Long id, @RequestBody Category category) {
        return ApiResult.ok(service.updateCategory(id, category));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResult<Void> deleteCategory(@PathVariable Long id) {
        service.deleteCategory(id);
        return ApiResult.ok();
    }

    @PostMapping("/internal/release")
    public ApiResult<Void> release(@RequestBody List<StockItem> items) {
        service.release(items);
        return ApiResult.ok();
    }

    @PostMapping("/internal/deduct")
    public ApiResult<Void> deduct(@RequestBody List<StockItem> items) {
        service.deduct(items);
        return ApiResult.ok();
    }
}