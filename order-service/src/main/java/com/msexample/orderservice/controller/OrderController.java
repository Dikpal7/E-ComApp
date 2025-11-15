package com.msexample.orderservice.controller;

import com.msexample.orderservice.client.ProductClient;
import com.msexample.orderservice.client.UserClient;
import com.msexample.orderservice.entity.Order;
import com.msexample.orderservice.entity.ProductDTO;
import com.msexample.orderservice.entity.UserDTO;
import com.msexample.orderservice.repository.OrderRepository;
import feign.FeignException;
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
    private final UserClient userClient;
    private final ProductClient productClient;

    private final String USER_SERVICE = "http://localhost:8081/users";
    private final String PRODUCT_SERVICE = "http://localhost:8082/products";

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody Map<String,Object> req){
        Long userId = Long.valueOf(req.get("userId").toString());
        Long productId = Long.valueOf(req.get("productId").toString());
        int qty = Integer.parseInt(req.get("quantity").toString());

        // 1) Verify user exists
        try {
//            ResponseEntity<Map> userResp = restTemplate.getForEntity(USER_SERVICE + "/" + userId, Map.class);
            UserDTO user = userClient.getUserById(userId);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
        } catch (HttpClientErrorException e){
            return ResponseEntity.badRequest().body("User not found");
        }

        // 2) Reserve product stock
        Map<String,Integer> body = Map.of("quantity", qty);
        try {
            ProductDTO prod = productClient.reserveProduct(productId, body);
            double price = prod.getPrice();
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
            UserDTO user = userClient.getUserById(userId);
            resp.put("user", user);
            return ResponseEntity.ok(resp);

        }catch (FeignException.Conflict e) {
            // same as 409 CONFLICT (Insufficient stock)
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Insufficient stock");
        } catch (FeignException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Product not found");
        } catch (FeignException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Product service error from FeignException");
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

