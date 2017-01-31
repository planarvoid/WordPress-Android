package com.soundcloud.android.cast;

import static com.soundcloud.android.cast.CastProtocol.TAG;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.utils.Log;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.reflect.TypeToken;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;

public class CastJsonHandler {

    @VisibleForTesting static final String KEY_QUEUE_STATUS = "queue_status";

    private JsonTransformer jsonTransformer;

    @Inject
    public CastJsonHandler(JsonTransformer jsonTransformer) {
        this.jsonTransformer = jsonTransformer;
    }

    public JSONObject toJson(CastPlayQueue castPlayQueue) {
        try {
            return new JSONObject(jsonTransformer.toJson(castPlayQueue));
        } catch (ApiMapperException | JSONException exception) {
            Log.d(TAG, "Unable to create json object");
            throw new IllegalArgumentException();
        }
    }

    CastPlayQueue parseCastPlayQueue(JSONObject jsonObj) throws IOException, ApiMapperException, JSONException {
        String json = jsonObj.get(KEY_QUEUE_STATUS).toString();
        return jsonTransformer.fromJson(json, TypeToken.of(CastPlayQueue.class));
    }

    public String toString(CastMessage castMessage) throws ApiMapperException {
        return jsonTransformer.toJson(castMessage);
    }
}
