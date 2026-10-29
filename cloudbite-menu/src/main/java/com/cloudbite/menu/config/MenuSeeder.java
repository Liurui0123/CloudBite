package com.cloudbite.menu.config;

import com.cloudbite.menu.service.MenuService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class MenuSeeder implements CommandLineRunner {
    private final MenuService menuService;

    public MenuSeeder(MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public void run(String... args) {
        menuService.seed();
    }
}