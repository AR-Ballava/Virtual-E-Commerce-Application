package com.ar.notification.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetails {
    private String orderId;
    private String username;
    private String email;
//    private String status;
    private Integer price;
}
