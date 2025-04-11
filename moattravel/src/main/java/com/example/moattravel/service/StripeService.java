package com.example.moattravel.service;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.moattravel.form.ReservationRegisterForm;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;

@Service
public class StripeService {
	@Value("${stripe.api-key}")
	private String stripeApiKey;
	
	private final ReservationService reservationService;

	public StripeService(ReservationService reservationService) {
		this.reservationService = reservationService;
	}    

    // セッションを作成し、Stripeに必要な情報を返す
    public String createStripeSession(String houseName, ReservationRegisterForm reservationRegisterForm, HttpServletRequest httpServletRequest) {
    	Stripe.apiKey = stripeApiKey;
        String requestUrl = new String(httpServletRequest.getRequestURL());

        SessionCreateParams params = SessionCreateParams.builder()
            .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
            .addLineItem(
                SessionCreateParams.LineItem.builder()
                    .setPriceData(
                        SessionCreateParams.LineItem.PriceData.builder()
                            .setProductData(
                                SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(houseName)
                                    .build())
                            .setUnitAmount((long) reservationRegisterForm.getAmount())
                            .setCurrency("jpy")
                            .build())
                    .setQuantity(1L)
                    .build())
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setSuccessUrl(requestUrl.replaceAll("/houses/[0-9]+/reservations/confirm", "") + "/reservations?reserved")
            .setCancelUrl(requestUrl.replace("/reservations/confirm", ""))
            .setPaymentIntentData(
                SessionCreateParams.PaymentIntentData.builder()
                    .putMetadata("houseId", reservationRegisterForm.getHouseId().toString())
                    .putMetadata("userId", reservationRegisterForm.getUserId().toString())
                    .putMetadata("checkinDate", reservationRegisterForm.getCheckinDate())
                    .putMetadata("checkoutDate", reservationRegisterForm.getCheckoutDate())
                    .putMetadata("numberOfPeople", reservationRegisterForm.getNumberOfPeople().toString())
                    .putMetadata("amount", reservationRegisterForm.getAmount().toString())
                    .build())
            .build();

        try {
            Session session = Session.create(params);
            return session.getId();
        } catch (StripeException e) {
            e.printStackTrace();
            return "";
        }
    }
    
    // セッションから予約情報を取得し、ReservationServiceクラスを介してデータベースに登録する
//    public void processSessionCompleted(Event event) {
//
//    	System.out.println("test3");
//    	System.out.println(event);
//        Optional<StripeObject> optionalStripeObject = event.getDataObjectDeserializer().getObject();
//
//        System.out.println("test4");
//        System.out.println(optionalStripeObject);
//        optionalStripeObject.ifPresent(stripeObject -> {
//            Session session = (Session) stripeObject;
//            SessionRetrieveParams params = SessionRetrieveParams.builder()
//                .addExpand("payment_intent")
//                .build();
//
//            try {
//                session = Session.retrieve(session.getId(), params, null);
//                System.out.println("✔ セッション再取得成功");
//                
//                Map<String, String> paymentIntentObject = session.getPaymentIntentObject().getMetadata();
//                System.out.println("✔ メタデータ取得: " + paymentIntentObject);
//
//                reservationService.create(paymentIntentObject);
//                System.out.println("✔ 予約登録処理完了");
//            } catch (StripeException e) {
//                System.out.println("❌ StripeException 発生: " + e.getMessage());
//                e.printStackTrace();
//            } catch (Exception e) {
//                System.out.println("❌ その他の例外: " + e.getMessage());
//                e.printStackTrace();
//            }
//        });
//    }
    
    public void processSessionCompleted(String payload, Event event) {
        System.out.println("🟡 イベントタイプ: " + event.getType());

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();

        if (deserializer.getObject().isPresent()) {
            Session session = (Session) deserializer.getObject().get();
            System.out.println("✅ セッションオブジェクト取得済み（非推奨）: " + session);
        } else {
            // JacksonでJSONをパース
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(payload);

                String sessionId = rootNode
                        .path("data")
                        .path("object")
                        .path("id")
                        .asText();

                System.out.println("📄 セッションID: " + sessionId);

                // Stripe APIでセッション再取得
                Session session = Session.retrieve(
                    sessionId,
                    SessionRetrieveParams.builder()
                        .addExpand("payment_intent")
                        .build(),
                    null
                );

                System.out.println("✅ セッション再取得成功");

                Map<String, String> metadata = session.getPaymentIntentObject().getMetadata();
                System.out.println("📦 メタデータ: " + metadata);

                reservationService.create(metadata);
                System.out.println("✅ 予約作成成功");

            } catch (Exception e) {
                System.out.println("❌ エラー発生: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


}