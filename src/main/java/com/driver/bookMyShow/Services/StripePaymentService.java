package com.driver.bookMyShow.Services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

/**
 * StripePaymentService - Handles Stripe payment processing
 * 
 * Features:
 * - Create payment intent
 * - Retrieve payment status
 * - Confirm payment
 * 
 * NOTE: This uses Stripe TEST MODE (free) with test API keys
 * Test card: 4242 4242 4242 4242, any future expiry, any CVC
 */
@Service
public class StripePaymentService {

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    @Value("${STRIPE_SECRET_KEY:}")
    private String stripeSecretKeyEnv;

    @Value("${STRIPE_API_KEY:}")
    private String stripeApiKeyEnv;

    @PostConstruct
    public void init() {
        Stripe.apiKey = resolveStripeApiKey();
    }

    private String resolveStripeApiKey() {
        if (stripeApiKey != null && !stripeApiKey.trim().isBlank()) {
            return stripeApiKey.trim();
        }

        if (stripeSecretKeyEnv != null && !stripeSecretKeyEnv.trim().isBlank()) {
            return stripeSecretKeyEnv.trim();
        }

        if (stripeApiKeyEnv != null && !stripeApiKeyEnv.trim().isBlank()) {
            return stripeApiKeyEnv.trim();
        }

        String systemEnvSecret = System.getenv("STRIPE_SECRET_KEY");
        if (systemEnvSecret != null && !systemEnvSecret.trim().isBlank()) {
            return systemEnvSecret.trim();
        }

        String systemEnvApi = System.getenv("STRIPE_API_KEY");
        if (systemEnvApi != null && !systemEnvApi.trim().isBlank()) {
            return systemEnvApi.trim();
        }

        String dotenvSecret = resolveFromDotenv("STRIPE_SECRET_KEY");
        if (dotenvSecret != null && !dotenvSecret.trim().isBlank()) {
            return dotenvSecret.trim();
        }

        String dotenvApi = resolveFromDotenv("STRIPE_API_KEY");
        if (dotenvApi != null && !dotenvApi.trim().isBlank()) {
            return dotenvApi.trim();
        }

        return "";
    }

    private String resolveFromDotenv(String key) {
        List<String> searchDirectories = List.of(
                "./",
                "./Book-My-Show",
                "../Book-My-Show"
        );

        for (String directory : searchDirectories) {
            try {
                Dotenv dotenv = Dotenv.configure()
                        .directory(directory)
                        .ignoreIfMissing()
                        .load();
                String value = dotenv.get(key);
                if (value != null && !value.trim().isBlank()) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }

        return "";
    }

    private void ensureStripeConfigured() {
        if (Stripe.apiKey == null || Stripe.apiKey.isBlank()) {
            Stripe.apiKey = resolveStripeApiKey();
        }
        if (Stripe.apiKey == null || Stripe.apiKey.isBlank()) {
            throw new IllegalStateException("Stripe secret key is not configured");
        }
    }

    /**
     * Create a payment intent with Stripe
     * @param amount Amount in smallest currency unit (paise for INR, cents for USD)
     * @param currency Currency code (inr, usd, etc.)
     * @return PaymentIntent with client secret
     */
    public PaymentIntent createPaymentIntent(Long amount, String currency) throws StripeException {
        ensureStripeConfigured();
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .addPaymentMethodType("card")
                .build();

        return PaymentIntent.create(params);
    }

    /**
     * Create payment intent with metadata
     */
    public PaymentIntent createPaymentIntent(Long amount, String currency, Map<String, String> metadata) 
            throws StripeException {
        ensureStripeConfigured();
        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .addPaymentMethodType("card");

        if (metadata != null && !metadata.isEmpty()) {
            paramsBuilder.putAllMetadata(metadata);
        }

        return PaymentIntent.create(paramsBuilder.build());
    }

    /**
     * Retrieve payment intent by ID
     */
    public PaymentIntent retrievePaymentIntent(String paymentIntentId) throws StripeException {
        ensureStripeConfigured();
        return PaymentIntent.retrieve(paymentIntentId);
    }

    /**
     * Cancel payment intent
     */
    public PaymentIntent cancelPaymentIntent(String paymentIntentId) throws StripeException {
        ensureStripeConfigured();
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return paymentIntent.cancel();
    }
}
