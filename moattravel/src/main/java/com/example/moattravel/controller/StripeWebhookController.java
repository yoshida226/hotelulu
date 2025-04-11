package com.example.moattravel.controller;

import java.io.IOException;
import java.util.Scanner;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import com.example.moattravel.service.StripeService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;

@Controller
public class StripeWebhookController {

    private final StripeService stripeService;

    @Value("${stripe.api-key}")
    private String stripeApiKey;

    @Value("${stripe.webhook-secret}")
    private String webhookSecret;

    public StripeWebhookController(StripeService stripeService) {
        this.stripeService = stripeService;
    }

    @PostMapping("/stripe/webhook")
//    public ResponseEntity<String> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
    public ResponseEntity<String> webhook(HttpServletRequest request, @RequestHeader("Stripe-Signature") String sigHeader) {
        String payload = "";

        try (Scanner s = new Scanner(request.getInputStream()).useDelimiter("\\A")) {
            payload = s.hasNext() ? s.next() : "";
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to read payload");
        }

        System.out.println("Received raw payload: " + payload);
    
    	System.out.println("test");
        System.out.println("payload: " + payload);
        System.out.println("sigHeader: " + sigHeader);
        
        Stripe.apiKey = stripeApiKey;
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        System.out.println(event.getType());
//        if ("checkout.session.completed".equals(event.getType())) {
//        	System.out.println("test1");
//        	System.out.println(event);
//            stripeService.processSessionCompleted(event);
//            System.out.println("test2");
//        }
        
        if ("checkout.session.completed".equals(event.getType())) {
            stripeService.processSessionCompleted(payload, event); // ← payload 渡す
        }

        return new ResponseEntity<>("Success", HttpStatus.OK);
    }
    

}