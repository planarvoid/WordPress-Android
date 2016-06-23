package com.soundcloud.android.payments.googleplay;

import com.soundcloud.android.payments.ProductDetails;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;

class ResponseProcessor {

    @Inject
    ResponseProcessor() {
    }

    public ProductDetails parseProduct(String productJson) throws JSONException {
        JSONObject json = new JSONObject(productJson);
        return new ProductDetails(json.getString("productId"),
                                  BillingUtil.removeAppName(json.optString("title")),
                                  json.optString("description"),
                                  json.optString("price"));
    }

    public String extractToken(String json) throws JSONException {
        return new JSONObject(json).getString("developerPayload");
    }

}
