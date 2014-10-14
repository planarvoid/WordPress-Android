package com.soundcloud.android.payments.googleplay;

import com.soundcloud.android.payments.ProductDetails;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

class PlayResponseProcessor {

    @Inject
    PlayResponseProcessor() {}

    public ProductDetails parseProduct(String productJson) throws JSONException {
        JSONObject json = new JSONObject(productJson);
        return new ProductDetails(json.optString("productId"),
                PlayBillingUtil.removeAppName(json.optString("title")),
                json.optString("description"),
                json.optString("price"));
    }

}
