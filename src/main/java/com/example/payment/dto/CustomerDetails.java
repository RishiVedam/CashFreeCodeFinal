package com.example.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CustomerDetails{


    @JsonProperty("customer_name")
    public String customerName;

    @JsonProperty("customer_id")
    public String customerId;

    @JsonProperty("customer_email")
    public String customerEmail;

    @JsonProperty("customer_phone")
    public String customerPhone;
}