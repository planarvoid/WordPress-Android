package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.payments.ProductDetails;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ResponseProcessorTest {

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

        expect(details.getId()).toEqual("product_id");
        expect(details.getTitle()).toEqual("subscription title");
        expect(details.getDescription()).toEqual("placeholder description");
        expect(details.getPrice()).toEqual("€4.75");
    }

    @Test
    public void extractsTokenFromPurchaseJson() throws JSONException {
        String token = processor.extractToken(PURCHASE_JSON);
        expect(token).toEqual("soundcloud:payments:orders:e72661985b8911e49a4200e081c198e9");
    }

}