package com.msexample.orderservice.entity;

import lombok.Data;

@Data
public class ProductDTO {
    private Long id;
    private String name;
    private double price;
    private Integer stock;
}
