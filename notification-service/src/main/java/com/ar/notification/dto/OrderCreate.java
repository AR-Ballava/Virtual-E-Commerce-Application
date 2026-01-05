package com.ar.notification.dto;

public record OrderCreate(String orderId, String name, String email, String status, int price) {
}
