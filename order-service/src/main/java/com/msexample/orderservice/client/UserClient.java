package com.msexample.orderservice.client;

import com.msexample.orderservice.entity.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service")
public interface UserClient {

    @GetMapping("/users/{id}")
    public UserDTO getUserById(@PathVariable("id") long id);
}
