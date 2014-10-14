package com.soundcloud.android.payments.googleplay;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.payments.ProductDetails;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class PlayResponseProcessorTest {

    private static final String PRODUCT_JSON = "{\"title\":\"subscription title\",\"price\":\"€4.75\",\"type\":\"subs\",\"description\":\"placeholder description\",\"price_amount_micros\":4750000,\"price_currency_code\":\"EUR\",\"productId\":\"consumer_subscription\"}";

    private PlayResponseProcessor processor;

    @Before
    public void setUp() throws Exception {
        processor = new PlayResponseProcessor();
    }

    @Test
    public void parsesProductDetailsFromJson() throws JSONException {
        ProductDetails details = processor.parseProduct(PRODUCT_JSON);

        expect(details.id).toEqual("consumer_subscription");
        expect(details.title).toEqual("subscription title");
        expect(details.description).toEqual("placeholder description");
        expect(details.price).toEqual("€4.75");
    }
}