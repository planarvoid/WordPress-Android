package com.soundcloud.android.payments.googleplay;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.payments.ProductDetails;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

public class ResponseProcessorTest extends PlatformUnitTest {

    // Real responses from Google Play billing service
    private static final String PRODUCT_JSON = "{\"title\":\"subscription title\",\"price\":\"€4.75\",\"type\":\"subs\",\"description\":\"placeholder description\",\"price_amount_micros\":4750000,\"price_currency_code\":\"EUR\",\"productId\":\"product_id\"}";
    private static final String PURCHASE_JSON = "{\"orderId\":\"12999763169054705758.1343597682978365\",\"packageName\":\"com.soundcloud.android\",\"productId\":\"android_test_product\",\"purchaseTime\":1414161345678,\"purchaseState\":0,\"developerPayload\":\"soundcloud:payments:orders:e72661985b8911e49a4200e081c198e9\",\"purchaseToken\":\"objhhcjhlomeghckclmnmnfe.AO-J1OxNatDmZqPUrXFbudWE4lIDGUxeuCU_nDxKY2FconuT_ikiUIQvSFi5Q1FLhUp2fvtu9P5ahPHI2V-TCb6Rg74Oo-htz0CAUSme96paccFu1IepnDYmY7Apk-30Wl-800DTA6v9\"}";

    private ResponseProcessor processor;

    @Before
    public void setUp() throws Exception {
        processor = new ResponseProcessor();
    }

    @Test
    public void parsesProductDetailsFromJson() throws JSONException {
        ProductDetails details = processor.parseProduct(PRODUCT_JSON);

        assertThat(details.getId()).isEqualTo("product_id");
        assertThat(details.getTitle()).isEqualTo("subscription title");
        assertThat(details.getDescription()).isEqualTo("placeholder description");
        assertThat(details.getPrice()).isEqualTo("€4.75");
    }

    @Test
    public void extractsTokenFromPurchaseJson() throws JSONException {
        String token = processor.extractToken(PURCHASE_JSON);
        assertThat(token).isEqualTo("soundcloud:payments:orders:e72661985b8911e49a4200e081c198e9");
    }

}