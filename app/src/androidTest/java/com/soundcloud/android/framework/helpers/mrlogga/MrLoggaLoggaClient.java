package com.soundcloud.android.framework.helpers.mrlogga;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.DeviceHelper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;

class MrLoggaLoggaClient {
    private static final String TAG = MrLoggaVerifier.class.getSimpleName();

    private static final MediaType MEDIA_TYPE_PLAIN_TEXT = MediaType.parse("text/plain");
    private static final String PARAM_ANONYMOUS_ID = "anonymous_id";
    private static final String PARAM_SCENARIO_ID = "scenario_id";

    private static final String recordingEndpoint = "http://localhost:4567/";

    private static final String ACTION_START_LOGGING = "start_logging";
    private static final String ACTION_FINISH_LOGGING = "finish_logging";
    private static final String ACTION_VALIDATION = "validation";
    private static final String ACTION_START_RECORDING = "start_recording";
    private static final String ACTION_FINISH_RECORDING = "finish_recording";
    private static final String IS_SCENARIO_COMPLETE_SCENARIO_ID = "is_scenario_complete";
    private static final String IS_FINISHED_LOGGING = "true";

    private final OkHttpClient httpClient;
    private final String loggingEndpoint;

    public final String deviceUDID;

    public MrLoggaLoggaClient(Context context, DeviceHelper deviceHelper, OkHttpClient client) {
        this.httpClient = client;
        this.deviceUDID = deviceHelper.getUdid();
        this.loggingEndpoint = getLoggingEndpoint(context);
    }

    private String getLoggingEndpoint(Context c) {
        Uri uri = Uri.parse(c.getString(R.string.eventgateway_url));
        return uri.getScheme() + "://" + uri.getAuthority() + "/";
    }

    MrLoggaResponse startLogging() {
        return sendPostLoggingRequest(ACTION_START_LOGGING);
    }

    boolean isScenarioComplete(String scenarioId) {
        final Request request = new Request.Builder()
                .url(buildIsScenarioCompletedUrl(scenarioId))
                .get()
                .build();
        return executeRequest(request).responseBody.equals(IS_FINISHED_LOGGING);
    }

    MrLoggaResponse stopLogging() {
        return sendPostLoggingRequest(ACTION_FINISH_LOGGING);
    }

    MrLoggaResponse validate(String scenarioId) {
        return sendValidateRequest(scenarioId);
    }

    @SuppressWarnings("unused")
    public MrLoggaResponse startRecording(String scenarioId) {
        final Request request = new Request.Builder()
                .url(recordingEndpoint + ACTION_START_RECORDING)
                .post(RequestBody.create(MEDIA_TYPE_PLAIN_TEXT, scenarioId))
                .build();
        return executeRequest(request);
    }

    @SuppressWarnings("unused")
    public MrLoggaResponse finishRecording() {
        final Request request = new Request.Builder()
                .url(recordingEndpoint + ACTION_FINISH_RECORDING)
                .post(null)
                .build();
        return executeRequest(request);
    }

    private MrLoggaResponse sendValidateRequest(String scenarioId) {
        final Request request = new Request.Builder()
                .url(buildIsValidationUrl(scenarioId))
                .get().build();
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

    private MrLoggaResponse sendPostLoggingRequest(String action) {
        final Request request = new Request.Builder()
                .url(loggingEndpoint + action)
                .post(RequestBody.create(MEDIA_TYPE_PLAIN_TEXT, deviceUDID))
                .build();
        return executeRequest(request);
    }

    private MrLoggaResponse executeRequest(Request request) {
        try {
            Response response = httpClient.newCall(request).execute();
            return new MrLoggaResponse(response.isSuccessful(), response.body().string());
        } catch (IOException exception) {
            Log.e(TAG, "IOException when request", exception);
            return new MrLoggaResponse(false, exception.getMessage());
        }
    }


}
