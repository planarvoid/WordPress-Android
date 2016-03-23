package com.soundcloud.android.tests.payments;

import com.soundcloud.android.BuildConfig;

import java.util.Random;

final class FakeResult {

    public final String orderId;
    public final String packageName;
    public final String productId;
    public final long purchaseTime;
    public final int purchaseState;
    public final String developerPayload;
    public final String purchaseToken;

    public static FakeResult valid(String checkoutToken) {
        return new FakeResult(checkoutToken, "VALID_");
    }

    public static FakeResult invalid(String checkoutToken) {
        return new FakeResult(checkoutToken, "INVALID_");
    }

    private FakeResult(String checkoutToken, String tokenPrefix) {
        long time = System.currentTimeMillis();
        orderId = generateOrderId();
        packageName = BuildConfig.APPLICATION_ID;
        productId = "android_test_product";
        purchaseTime = time;
        purchaseState = 0;
        developerPayload = checkoutToken;
        purchaseToken = tokenPrefix + time;
    }

    private String generateOrderId() {
        return randomDigits(21) + "." + randomDigits(16);
    }

    private String randomDigits(int size) {
        Random random = new Random();
        String digits = "";
        for (int i = 0; i < size; i++) {
            digits += random.nextInt(9);
        }
        return digits;
    }

}
