package com.example.payment.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Payment{
    @JsonProperty("cf_payment_id")
    public long cfPaymentId;

    @JsonProperty("payment_status")
    public String paymentStatus;

    @JsonProperty("payment_amount")
    public double paymentAmount;

    @JsonProperty("payment_currency")
    public String paymentCurrency;

    @JsonProperty("payment_message")
    public String paymentMessage;

    @JsonProperty("payment_time")
    public Date paymentTime;

    @JsonProperty("bank_reference")
    public String bankReference;

    @JsonProperty("auth_id")
    public Object authId;

    @JsonProperty("payment_method")
    public PaymentMethod paymentMethod;

    @JsonProperty("payment_group")
    public String paymentGroup;
}