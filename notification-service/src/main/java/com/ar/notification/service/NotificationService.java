package com.ar.notification.service;

import com.ar.notification.entity.OrderDetails;
import com.ar.order.event.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    private final RestClient restClient;

    @KafkaListener(topics = "vec-order-topic", groupId = "veg-og")
    public void sendMail(OrderPlacedEvent event, Acknowledgment ack) {
        try {
            OrderDetails orderDetails = getOrderDetails(event);

            MimeMessagePreparator messagePreparator = mimeMessage -> {
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
                helper.setFrom("ImRich");
                helper.setTo(orderDetails.getEmail());
                helper.setSubject(
                        String.format("Order with order no %s Placed Successfully", orderDetails.getOrderId())
                );
                helper.setText(String.format("""
                Hi, %s

                Congratulations! Your order with order number %s has been successfully placed.
                For more details, check your collections.

                Best Regards
                ImRich
                """, orderDetails.getUsername(), orderDetails.getOrderId()));
            };

            mailSender.send(messagePreparator);
            log.info("Mail sent successfully for order {}", orderDetails.getOrderId());

        } catch (HttpClientErrorException.NotFound ex) {
            // Business case: product/order data not found
            log.warn("Order details not found for event {}, skipping mail", event);

        } catch (Exception ex) {
            // Unexpected errors (mail server down, timeout, etc.)
            log.error("Failed to send mail for event {}", event, ex);

        } finally {
            // 🔑 CRITICAL: always acknowledge
            ack.acknowledge();
        }
    }

    public OrderDetails  getOrderDetails(OrderPlacedEvent event){
        OrderDetails orderDetails = new OrderDetails();


        try {
            Integer price = restClient.get()
                    .uri("http://localhost:8083/api/product/getPrice/{productId}", event.getProductId())
                    .retrieve()
                    .body(Integer.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        try {
            String username = restClient.get()
                    .uri("http://localhost:8082/api/user/getName/{userId}", event.getUserId())
                    .retrieve()
                    .body(String.class); orderDetails.setUsername(username);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            String email = restClient.get()
                    .uri("http://localhost:8082/api/user/getEmail/{userId}", event.getUserId())
                    .retrieve()
                    .body(String.class); orderDetails.setEmail(email);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        orderDetails.setOrderId(event.getOrderId());
        return orderDetails;
    }
}