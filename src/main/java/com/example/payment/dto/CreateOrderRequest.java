package com.example.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    @JsonProperty("order_id")
    private String orderId;

    @JsonProperty("order_amount")
    private Double orderAmount;

    @JsonProperty("order_currency")
    private String orderCurrency;

    @JsonProperty("order_expiry_time")
    private String orderExpiryTime;

    @JsonProperty("customer_details")
    private CustomerDetails customerDetails;

    @JsonProperty("order_meta")
    private OrderMeta orderMeta;
}
