package com.msexample.orderservice.client;

import com.msexample.orderservice.entity.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.Map;

@FeignClient(name = "product-service")
public interface ProductClient {

    @PostMapping("/products/{id}/reserve")
    ProductDTO reserveProduct(@PathVariable("id") Long id, @RequestBody Map<String, Integer> request);
}
