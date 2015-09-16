package com.soundcloud.android.facebookapi;

import com.facebook.GraphResponse;
import org.json.JSONObject;

import android.support.annotation.VisibleForTesting;

class FacebookApiResponse {
    private boolean isError;
    private JSONObject jsonObject;

    FacebookApiResponse(GraphResponse graphResponse) {
        this.isError = graphResponse.getError() != null;
        this.jsonObject = graphResponse.getJSONObject();
    }

    @VisibleForTesting
    FacebookApiResponse(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    @VisibleForTesting
    FacebookApiResponse(boolean isError) {
        this.isError = isError;
    }

    boolean isSuccess() {
        return !isError;
    }

    JSONObject getJSONObject() {
        return jsonObject;
    }

}
