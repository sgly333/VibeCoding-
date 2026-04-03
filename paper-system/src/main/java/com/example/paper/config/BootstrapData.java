package com.example.paper.config;

import com.example.paper.entity.AppUser;
import com.example.paper.entity.Category;
import com.example.paper.repository.AppUserRepository;
import com.example.paper.repository.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Component
public class BootstrapData implements CommandLineRunner {

    private static final List<String> DEFAULT_CATEGORIES = List.of(
            "CF Based",
            "Graph Based",
            "Context Based",
            "Hybrid Based",
            "LLM Based"
    );

    private final CategoryRepository categoryRepository;
    private final AppUserRepository appUserRepository;

    public BootstrapData(CategoryRepository categoryRepository, AppUserRepository appUserRepository) {
        this.categoryRepository = categoryRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            for (String name : DEFAULT_CATEGORIES) {
                Category category = new Category();
                category.setName(name);
                categoryRepository.save(category);
            }
        }

        if (appUserRepository.count() == 0) {
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            AppUser admin = new AppUser();
            admin.setUsername("admin");
            admin.setPasswordHash(encoder.encode("admin123"));
            appUserRepository.save(admin);
        }
    }
}

