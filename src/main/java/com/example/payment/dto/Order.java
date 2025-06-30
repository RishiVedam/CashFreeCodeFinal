package com.example.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order{
    @JsonProperty("order_id")
    public String orderId;

    @JsonProperty("order_amount")
    public double orderAmount;

    @JsonProperty("order_currency")
    public String orderCurrency;

    @JsonProperty("order_tags")
    public Object orderTags;
}
