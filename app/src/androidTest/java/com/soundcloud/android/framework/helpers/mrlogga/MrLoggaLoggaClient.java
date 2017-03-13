package com.soundcloud.android.framework.helpers.mrlogga;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.reflect.TypeToken;
import com.soundcloud.java.strings.Strings;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import android.content.Context;
import android.net.Uri;

import java.io.IOException;

class MrLoggaLoggaClient {
    private static final String TAG = MrLoggaVerifier.class.getSimpleName();

    private static final MediaType MEDIA_TYPE_PLAIN_TEXT = MediaType.parse("text/plain");
    private static final RequestBody EMPTY_REQUEST_BODY = RequestBody.create(MEDIA_TYPE_PLAIN_TEXT, "");
    private static final String PARAM_ANONYMOUS_ID = "anonymous_id";
    private static final String PARAM_SCENARIO_ID = "scenario_id";

    private static final String ACTION_START_LOGGING = "start_logging";
    private static final String ACTION_FINISH_LOGGING = "finish_logging";
    private static final String ACTION_VALIDATION = "validation";
    private static final String ACTION_START_RECORDING = "start_recording";
    private static final String ACTION_FINISH_RECORDING = "finish_recording";
    private static final String IS_SCENARIO_COMPLETE_SCENARIO_ID = "is_scenario_complete";

    private final OkHttpClient httpClient;
    private final String loggingEndpoint;
    private final JsonTransformer jsonTransformer;
    protected final String deviceUDID;

    MrLoggaLoggaClient(Context context, DeviceHelper deviceHelper,
                       OkHttpClient client, JsonTransformer jsonTransformer) {
        this.httpClient = client;
        this.deviceUDID = deviceHelper.getUdid();
        this.loggingEndpoint = getLoggingEndpoint(context);
        this.jsonTransformer = jsonTransformer;
    }

    private String getLoggingEndpoint(Context c) {
        Uri uri = Uri.parse(c.getString(R.string.eventgateway_url));
        return uri.getScheme() + "://" + uri.getAuthority() + "/";
    }

    MrLoggaResponse startLogging() {
        return sendPostLoggingRequest(ACTION_START_LOGGING, Optional.of(System.currentTimeMillis()));
    }

    boolean isScenarioComplete(String scenarioId) {
        final Request request = new Request.Builder()
                .url(buildIsScenarioCompletedUrl(scenarioId))
                .get()
                .build();
        return Boolean.parseBoolean(executeRequest(request).responseBody);
    }

    MrLoggaResponse stopLogging() {
        return sendPostLoggingRequest(ACTION_FINISH_LOGGING, Optional.absent());
    }

    ValidationResponse validate(String scenarioId) throws ApiMapperException, IOException {
        final Request request = new Request.Builder()
                .url(buildIsValidationUrl(scenarioId))
                .get().build();
        return executeValidationRequest(request);
    }

    @SuppressWarnings("unused")
    MrLoggaResponse startRecording(String scenarioId) {
        final Request request = new Request.Builder()
                .url(loggingEndpoint + ACTION_START_RECORDING)
                .post(RequestBody.create(MEDIA_TYPE_PLAIN_TEXT, scenarioId))
                .build();
        return executeRequest(request);
    }

    @SuppressWarnings("unused")
    MrLoggaResponse finishRecording() {
        final Request request = new Request.Builder()
                .url(loggingEndpoint + ACTION_FINISH_RECORDING)
                .post(EMPTY_REQUEST_BODY)
                .build();
        return executeRequest(request);
    }

    private String buildIsScenarioCompletedUrl(String scenarioId) {
        return buildValidationUrl(IS_SCENARIO_COMPLETE_SCENARIO_ID, scenarioId);
    }

    private String buildIsValidationUrl(String scenarioId) {
        return buildValidationUrl(ACTION_VALIDATION, scenarioId);
    }

    private String buildValidationUrl(String action, String scenarioId) {
        return Uri.parse(loggingEndpoint).buildUpon().appendPath(action)
                  .appendQueryParameter(PARAM_ANONYMOUS_ID, deviceUDID)
                  .appendQueryParameter(PARAM_SCENARIO_ID, scenarioId)
                  .build().toString();
    }

    private MrLoggaResponse sendPostLoggingRequest(String action, Optional<Long> timestamp) {
        final String timestampParam = timestamp.isPresent() ? "?timestamp=" + timestamp.get() : Strings.EMPTY;
        final Request request = new Request.Builder()
                .url(loggingEndpoint + action + timestampParam)
                .post(RequestBody.create(MEDIA_TYPE_PLAIN_TEXT, deviceUDID))
                .build();
        return executeRequest(request);
    }

    private MrLoggaResponse executeRequest(Request request) {
        try {
            Response response = httpClient.newCall(request).execute();
            final String responsePayload = response.body().string();
            return new MrLoggaResponse(response.isSuccessful(), responsePayload);
        } catch (IOException exception) {
            return new MrLoggaResponse(false, exception.getMessage());
        }
    }

    private ValidationResponse executeValidationRequest(Request request) throws ApiMapperException, IOException {
        Response response = httpClient.newCall(request).execute();
        final String responseBody = response.body().string();
        if (response.isSuccessful()) {
            return new SuccessfulValidationResponse();
        } else {
            return jsonTransformer.fromJson(responseBody, TypeToken.of(FailedValidationResponse.class));
        }
    }


}
