package com.example.payment.controller;

import com.example.payment.dto.CreateOrderUIRequest;
import com.example.payment.dto.SaveCustomerRequest;
import com.example.payment.dto.WebHookPayload;
import com.example.payment.entity.OrderEntity;
import com.example.payment.repository.OrderRepository;
import com.example.payment.service.CashfreeService;
import com.example.payment.utils.TraceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    @Autowired
    CashfreeService cashfreeService;

    @Autowired
    OrderRepository orderRepository;

    @PostMapping("/save")
    public ResponseEntity<String> saveCustomer(@RequestBody SaveCustomerRequest request) {
//        log.info("Mdc :{}",MDC.getCopyOfContextMap());
//        log.atInfo().addKeyValue("customerId",request.getCustomerId()).log("saving the user to DB");
        log.info("saving the user to DB with customerId:{}",request.getCustomerId());
        cashfreeService.saveCustomer(request);
    return ResponseEntity.ok("Customer saved successfully");
    }

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderUIRequest request) {
//        log.atInfo().addKeyValue("customerId",request.getCustomerId()).log("Creating order");
        log.info("creating order for customerId:{}",request.getCustomerId());
        return ResponseEntity.ok(cashfreeService.createOrder(request));
    }

    @GetMapping("/{orderId}/verify")
    public ResponseEntity<?> getOrderStatus(@PathVariable String orderId) {
//        log.atInfo().addKeyValue("orderId",orderId).log("goining to find order using orderId");
        log.info("goining to find order using orderId{}",orderId);
        OrderEntity order = orderRepository.findByOrderId(orderId);
        if (order == null) {
//        log.atInfo().addKeyValue("orderId",orderId).log("not found order with given orderId");
            log.info("not found order with given orderId{}",orderId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No order found for this orderId");
        }
        cashfreeService.updatePaymentStatus(order.getOrderId());
        OrderEntity updatedOrder = orderRepository.findByOrderId(order.getOrderId());
        if (updatedOrder.getStatus() != null) {
            return ResponseEntity.ok(Map.of("status", updatedOrder.getStatus()));
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "Could not retrieve payment status."));
        }
    }

    @PostMapping("/status/webhook")
    public ResponseEntity<Void> getWebhookStatus(@RequestBody WebHookPayload payload)
    {
        String orderId       = payload.getData().getOrder().orderId;
        String paymentStatus = payload.getData().getPayment().paymentStatus;
        OrderEntity order = orderRepository.findByOrderId(orderId);
        if (order == null) {
//            log.atWarn().addKeyValue("orderId",orderId).log("Webhook received for unknown orderId");
//            log.warn("Webhook received for unknown orderId{}",orderId);

            TraceUtil.ensureTraceContext();
            try {
                log.warn("Webhook received for unknown orderId{}",orderId);
            } finally {
                TraceUtil.clear();
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        order.setStatus(paymentStatus);
        orderRepository.save(order);
        log.atInfo()
                .addKeyValue("orderId", orderId)
                .addKeyValue("paymentStatus", paymentStatus)
                .addKeyValue("feeType", order.getFeeType())
                .addKeyValue("account", order.getCashfreeAccount())
                .log();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/thread-test")
    public String testThreads() {
        log.info("Mdc :{}",MDC.getCopyOfContextMap());
        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
//                log.atWarn().addKeyValue("thread", Thread.currentThread().getName()).log("Thread running");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            t.setName("TestThread-" + i);
            t.start();
        }
        return "Threads started";
    }

}
