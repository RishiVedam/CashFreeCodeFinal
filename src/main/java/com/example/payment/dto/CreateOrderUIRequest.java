package com.example.payment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateOrderUIRequest {
    private Double orderAmount;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerId;

    private FeeType feeType;
}




