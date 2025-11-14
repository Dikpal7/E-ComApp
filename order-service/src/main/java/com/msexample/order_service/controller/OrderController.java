package com.msexample.order_service.controller;

import com.msexample.order_service.entity.Order;
import com.msexample.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository orderRepo;
    private final RestTemplate restTemplate;

    private final String USER_SERVICE = "http://localhost:8081/users";
    private final String PRODUCT_SERVICE = "http://localhost:8082/products";

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String,Object> req){
        Long userId = Long.valueOf(req.get("userId").toString());
        Long productId = Long.valueOf(req.get("productId").toString());
        int qty = Integer.parseInt(req.get("quantity").toString());

        // 1) Verify user exists
        try {
            ResponseEntity<Map> userResp = restTemplate.getForEntity(USER_SERVICE + "/" + userId, Map.class);
            if(!userResp.getStatusCode().is2xxSuccessful()) return ResponseEntity.badRequest().body("User invalid");
        } catch (HttpClientErrorException e){
            return ResponseEntity.badRequest().body("User not found");
        }

        // 2) Reserve product stock
        Map<String,Integer> body = Map.of("quantity", qty);
        try {
            ResponseEntity<Map> reserveResp = restTemplate.postForEntity(PRODUCT_SERVICE + "/" + productId + "/reserve", body, Map.class);
            if(!reserveResp.getStatusCode().is2xxSuccessful()){
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Insufficient stock");
            }
            Map prod = reserveResp.getBody();
            double price = Double.parseDouble(prod.get("price").toString());
            double total = price * qty;
            Order order = Order.builder()
                    .userId(userId)
                    .productId(productId)
                    .quantity(qty)
                    .totalPrice(total)
                    .status("PLACED")
                    .build();
            Order saved = orderRepo.save(order);

            // Compose response including user & product minimal info
            Map<String,Object> resp = new HashMap<>();
            resp.put("order", saved);
            resp.put("product", prod);
            // fetch user details
            ResponseEntity<Map> userDetails = restTemplate.getForEntity(USER_SERVICE + "/" + userId, Map.class);
            resp.put("user", userDetails.getBody());
            return ResponseEntity.ok(resp);

        } catch (HttpClientErrorException.Conflict e){
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Insufficient stock");
        } catch (Exception e){
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id){ return orderRepo.findById(id).orElseThrow(); }

    @GetMapping("/user/{userId}")
    public List<Order> ordersForUser(@PathVariable Long userId){
        return orderRepo.findAll().stream().filter(o -> o.getUserId().equals(userId)).collect(Collectors.toList());
    }
}

