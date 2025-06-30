package com.example.payment.service;

import com.example.payment.client.CashfreeClient;
import com.example.payment.config.CashfreeConfig;
import com.example.payment.dto.*;
import com.example.payment.entity.OrderEntity;
import com.example.payment.entity.OrderPaymentEntity;
import com.example.payment.repository.OrderPaymentRepository;
import com.example.payment.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashfreeService {

    @Autowired
    CashfreeClient cashfreeClient;

    @Autowired
    CashfreeConfig config;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderPaymentRepository orderPaymentRepository;

    @Autowired
    CashfreeAccountService accountService;

    public void updatePendingPayments() {
        List<OrderEntity> pendingOrders = orderRepository.findByStatus("PENDING");

        for (OrderEntity order : pendingOrders) {
            updatePaymentStatus(order.getOrderId());
        }
    }

    public void saveCustomer(SaveCustomerRequest request) {
//        log.atInfo().addKeyValue("CustomerId",request.getCustomerId()).log("finding customer with customerId");
        log.info("finding customer with customerId:{}",request.getCustomerId());
        Optional<OrderEntity> orderStatus = orderRepository.findByCustomerIdAndFeeType(
                request.getCustomerId(), request.getFeeType());

        if (orderStatus.isEmpty()) {
            String orderId = "ORDER_" + UUID.randomUUID();
            OrderEntity order = OrderEntity.builder()
                    .orderId(orderId)
                    .customerId(request.getCustomerId())
                    .customerName(request.getCustomerName())
                    .customerEmail(request.getCustomerEmail())
                    .customerPhone(request.getCustomerPhone())
                    .orderAmount(request.getOrderAmount())
                    .currency("INR")
                    .status("PENDING")
                    .createdAt(LocalDateTime.now())
                    .feeType(request.getFeeType())
                    .cashfreeAccount(accountService.getAccountKeyForFee(request.getFeeType()))
                    .build();

            orderRepository.save(order);
//            log.atInfo().addKeyValue("orderId",orderId).log("Saved user with orderId");
            log.info("Saved user with orderId:{}",orderId);
        } else {
//            log.atInfo().addKeyValue("CustomerId",request.getCustomerId()).log("user already existed with customerId: ");
            log.info("user already existed with customerId:{}",request.getCustomerId());
        }
    }

    public Object createOrder(CreateOrderUIRequest request) {

//        log.atInfo().addKeyValue("CustomerId",request.getCustomerId()).log("Fetching customer record with customerId");
        log.info("Fetching customer record with customerId:{}",request.getCustomerId());

        OrderEntity customerRecord = orderRepository
                .findByCustomerIdAndFeeType(request.getCustomerId(), request.getFeeType())
                .orElseThrow(() -> new RuntimeException("No record found for this customerId and fee type"));

        if (customerRecord.getSessionId() != null) {
            return new CreateOrderResponse(customerRecord.getSessionId(), customerRecord.getOrderId());
        }

        String orderId = customerRecord.getOrderId();
        OffsetDateTime expiryTime = OffsetDateTime.now().plusSeconds(12 * 60 * 60);


//        Map<String, Object> body = new HashMap<>();
//        body.put("order_id", orderId);
//        body.put("order_amount", customerRecord.getOrderAmount());
//        body.put("order_currency", "INR");
//        body.put("order_expiry_time", expiryTime);
//
//        Map<String, Object> customerDetails = new HashMap<>();
//        customerDetails.put("customer_id", customerRecord.getCustomerId());
//        customerDetails.put("customer_name", customerRecord.getCustomerName());
//        customerDetails.put("customer_email", customerRecord.getCustomerEmail());
//        customerDetails.put("customer_phone", customerRecord.getCustomerPhone());
//        body.put("customer_details", customerDetails);
//        Map<String, Object> orderMeta = new HashMap<>();
//        orderMeta.put("notify_url", "https://dca4-14-140-115-103.ngrok-free.app/api/orders/status/webhook");
//        body.put("order_meta", orderMeta);


        CustomerDetails customerDetails = CustomerDetails.builder()
                .customerId(customerRecord.getCustomerId())
                .customerName(customerRecord.getCustomerName())
                .customerEmail(customerRecord.getCustomerEmail())
                .customerPhone(customerRecord.getCustomerPhone())
                .build();
        OrderMeta orderMeta = OrderMeta.builder()
                .notify_url("https://dca4-14-140-115-103.ngrok-free.app/api/orders/status/webhook")
                .build();
        CreateOrderRequest body = CreateOrderRequest.builder()
                .orderId(orderId)
                .orderAmount(customerRecord.getOrderAmount())
                .orderCurrency("INR")
                .orderExpiryTime(String.valueOf(expiryTime))
                .customerDetails(customerDetails)
                .orderMeta(orderMeta)
                .build();


        CashfreeConfig.Credentials creds = accountService.getCredentialsForFee(customerRecord.getFeeType());

        CreateOrderResponse response = cashfreeClient.createOrder(
                body, creds.getClientId(), creds.getClientSecret(), "2025-01-01");

        if (response != null && response.getSessionId() != null) {
//            log.atInfo().addKeyValue("Setting sessionId",response.getSessionId()).addKeyValue("For customerId ",request.getCustomerId()).log();
            log.info("Setting sessionId:{} for customerId: {}",response.getSessionId(),request.getCustomerId());
            customerRecord.setSessionId(response.getSessionId());
//            log.atError().addKeyValue("CustomerId",request.getCustomerId()).log("No sessionId found for  customerId");
            log.info("Np sessionId found for customerId: {}",request.getCustomerId());
            customerRecord.setStatus("PENDING");
            orderRepository.save(customerRecord);
        } else {
//            log.atWarn().addKeyValue("CustomerId",request.getCustomerId()).log("No sessionId found for  customerId");
            log.warn("Np sessionId found for customerId: {}",request.getCustomerId());
            throw new RuntimeException("Failed to create Cashfree order: sessionId is null");
        }

        boolean skillPaid = orderRepository
                .existsByCustomerIdAndFeeTypeAndStatus(
                        customerRecord.getCustomerId(), FeeType.SKILL, "SUCCESS");

        boolean paymentAllowed =
                (customerRecord.getFeeType() == FeeType.SKILL) ||           // always allow skill fee
                        (customerRecord.getFeeType() == FeeType.COLLEGE && skillPaid);

        String sessionIdForClient = paymentAllowed ? customerRecord.getSessionId() : null;


        return new CreateOrderResponse(sessionIdForClient, customerRecord.getOrderId());

    }

    public void updatePaymentStatus(String orderId) {


        log.info("Updating PaymentStatus for orderId:{}", orderId);
        OrderEntity order = orderRepository.findByOrderId(orderId);
        if (order == null) {

            log.warn("Order not found locally for orderId:{}", orderId);
            return;
        }

        if (order.getSessionId() == null) {
            log.warn("Skipping orderId:{}", orderId);
            return;
        }

        List<Map<String, Object>> payments;
        try {
            CashfreeConfig.Credentials creds =
                    accountService.getCredentialsForFee(order.getFeeType());

            payments = cashfreeClient.getPaymentsByOrderId(
                    orderId,
                    creds.getClientId(),
                    creds.getClientSecret(),
                    "2025-01-01");

        } catch (feign.FeignException.NotFound nf) {
//            log.atWarn().addKeyValue("orderId",orderId).log("orderId not found on Cashfree");
            log.warn("orderId not found on Cashfree:{}", orderId);
            order.setStatus("PENDING");
            orderRepository.save(order);
            return;
        } catch (feign.FeignException fe) {
//            log.atWarn().addKeyValue("orderId",orderId).log("Cashfree error for orderId");
            log.warn("Cashfree error for orderId:{}", orderId);
            return;   // keep order as-is; try again next cycle
        }

        if (payments == null || payments.isEmpty()) {
//            log.atInfo().addKeyValue("orderId",orderId).log("No payments found for orderId");
            log.warn("No payments found for orderId:{}", orderId);
            return;
        }

        payments.stream()
                .map(p -> mapToPaymentEntity(order, p))
                .filter(Objects::nonNull)
                .filter(pe -> !orderPaymentRepository
                        .existsByPaymentIdAndPaymentStatus(
                                pe.getPaymentId(), pe.getPaymentStatus()))
                .forEach(orderPaymentRepository::save);

        boolean anySuccess = payments.stream()
                .anyMatch(p -> "SUCCESS".equalsIgnoreCase(
                        getAsString(p.get("payment_status"))));

        if (anySuccess) {
            order.setStatus("SUCCESS");
        } else {
            // latest status logic (unchanged)
            payments.sort((a, b) -> {
                try {
                    return Instant.parse(getAsString(b.get("payment_completion_time")))
                            .compareTo(
                                    Instant.parse(getAsString(a.get("payment_completion_time"))));
                } catch (Exception ex) {
                    return 0;
                }
            });
            order.setStatus(getAsString(payments.get(0).get("payment_status")));
        }
        orderRepository.save(order);

        log.info("Order status updated to :{} for orderId:{}", order.getStatus(), orderId);
    }

    private OrderPaymentEntity mapToPaymentEntity(OrderEntity order, Map<String, Object> payment) {
        try {
            String paymentId = getAsString(payment.get("cf_payment_id"));
            String status = getAsString(payment.get("payment_status"));
            String method = getAsString(payment.get("payment_method"));
            String bankRef = getAsString(payment.get("bank_reference"));
            String paymentTime = getAsString(payment.get("payment_completion_time"));

            if (paymentId == null || status == null || "NOT_ATTEMPTED".equalsIgnoreCase(status)) {
                return null;
            }

            return OrderPaymentEntity.builder()
                    .order(order)
                    .paymentId(paymentId)
                    .paymentStatus(status)
                    .paymentMethod(method)
                    .bankReference(bankRef)
                    .paymentTime(paymentTime)
                    .build();

        } catch (Exception e) {
            log.error("Error mapping payment entity for order {}: {}", order.getOrderId(), e.getMessage());
//            log.atInfo().addKeyValue("orderId",order.getOrderId()).log("Error mapping payment entity for orderId");
            return null;
        }
    }

    private String getAsString(Object obj) {
        if (obj instanceof String str) return str;
        if (obj instanceof Map<?, ?>) return obj.toString(); // Avoid ClassCastException
        return obj != null ? obj.toString() : null;
    }


}



