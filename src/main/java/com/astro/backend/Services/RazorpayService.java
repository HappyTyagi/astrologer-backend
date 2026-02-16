package com.astro.backend.Services;


import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RazorpayService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    public String createOrder(double amount, String currency) throws Exception {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        long amountPaise = Math.round(amount * 100.0);
        orderRequest.put("amount", amountPaise); // INR to paise
        orderRequest.put("currency", currency);
        orderRequest.put("payment_capture", 1);

//        Order order = client.Orders.create(orderRequest);
//        return order.toString();
        return "Done";
    }
}
