package com.msexample.user_service.controller;

import com.msexample.user_service.entity.User;
import com.msexample.user_service.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository repo;

    @PostMapping("/register")
    public User register(@RequestBody User u){
        return repo.save(u);
    }

    @GetMapping("/{id}")
    public User getUser(@PathVariable Long id){
        return repo.findById(id).orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<User> all() { return repo.findAll(); }
}
