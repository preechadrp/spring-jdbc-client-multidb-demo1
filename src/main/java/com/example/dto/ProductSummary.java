package com.example.dto;

//สร้างไว้ที่ไหนก็ได้ (เช่น ในโฟลเดอร์ dto หรือวางไว้ใต้ Product.java ก็ได้)
//JdbcClient .query(ProductSummary.class) จะ map คอลัมน์ id, name เข้า record ให้เอง
public record ProductSummary(Long id, String name) {
}
