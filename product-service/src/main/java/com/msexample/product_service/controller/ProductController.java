package com.msexample.product_service.controller;

import com.msexample.product_service.entity.Product;
import com.msexample.product_service.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductRepository repo;

    @GetMapping
    public List<Product> all() { return repo.findAll(); }

    @GetMapping("/{id}")
    public Product get(@PathVariable Long id) {
        return repo.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }

    @PostMapping
    public Product create(@RequestBody Product p) { return repo.save(p); }

    @PutMapping("/{id}/stock")
    public Product updateStock(@PathVariable Long id, @RequestParam int delta){
        Product p = repo.findById(id).orElseThrow();
        p.setStock(p.getStock() + delta);
        return repo.save(p);
    }

    // endpoint to reserve stock (atomic in service layer ideally)
    @PostMapping("/{id}/reserve")
    public ResponseEntity<?> reserve(@PathVariable Long id, @RequestBody Map<String,Integer> body){
        int qty = body.getOrDefault("quantity", 1);
        Product p = repo.findById(id).orElseThrow();
        if(p.getStock() < qty) return ResponseEntity.status(HttpStatus.CONFLICT).body("Insufficient stock");
        p.setStock(p.getStock() - qty);
        repo.save(p);
        return ResponseEntity.ok(p);
    }
}

