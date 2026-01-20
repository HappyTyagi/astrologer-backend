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

    public String createOrder(int amount, String currency) throws Exception {
        RazorpayClient client = new RazorpayClient(keyId, keySecret);

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amount * 100); // INR to paise
        orderRequest.put("currency", currency);
        orderRequest.put("payment_capture", 1);

//        Order order = client.Orders.create(orderRequest);
//        return order.toString();
        return "Done";
    }
}
