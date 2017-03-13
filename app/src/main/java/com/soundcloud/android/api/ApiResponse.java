package com.soundcloud.android.api;


import com.soundcloud.android.Consts;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.VisibleForTesting;

import java.net.HttpURLConnection;

public class ApiResponse {

    private static final String BAD_REQUEST_ERROR_KEY = "error_key";
    private static final String PUBLIC_API_ERRORS_KEY = "errors";
    private static final String PUBLIC_API_ERROR_KEY = "error";
    private static final String PUBLIC_API_ERROR_MESSAGE_KEY = "error_message";
    public static final int HTTP_UNPROCESSABLE_ENTITY = 422;
    private static final int HTTP_PRECONDITION_REQUIRED = 428;
    private static final int HTTP_REQUEST_TOO_MANY_REQUESTS = 429;

    private final int statusCode;
    private final String responseBody;
    @Nullable private ApiRequestException failure;

    @VisibleForTesting
    public ApiResponse(ApiRequest request, int statusCode, String responseBody) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        determineFailure(request, statusCode);
    }

    private void determineFailure(ApiRequest request, int statusCode) {
        if (statusCode == HTTP_REQUEST_TOO_MANY_REQUESTS) {
            failure = ApiRequestException.rateLimited(request, this, getErrorKey());
        } else if (statusCode == HTTP_PRECONDITION_REQUIRED) {
            failure = ApiRequestException.preconditionRequired(request, this);
        } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
            failure = ApiRequestException.notFound(request, this);
        } else if (statusCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            failure = ApiRequestException.authError(request, this);
        } else if (statusCode == HttpURLConnection.HTTP_FORBIDDEN) {
            failure = ApiRequestException.notAllowed(request, this);
        } else if (statusCode == HttpURLConnection.HTTP_BAD_REQUEST) {
            failure = ApiRequestException.badRequest(request, this, getErrorKey());
        } else if (statusCode == HTTP_UNPROCESSABLE_ENTITY) {
            failure = ApiRequestException.validationError(request, this, getErrorKey(), getErrorCode());
        } else if (statusCode >= HttpURLConnection.HTTP_INTERNAL_ERROR) {
            failure = ApiRequestException.serverError(request, this);
        } else if (!isSuccessCode(statusCode)) {
            failure = ApiRequestException.unexpectedResponse(request, this);
        }
    }

    public ApiResponse(ApiRequestException failure) {
        this.statusCode = Consts.NOT_SET;
        this.responseBody = Strings.EMPTY;
        this.failure = failure;
    }

    @Nullable
    public ApiRequestException getFailure() {
        return failure;
    }

    private boolean isSuccessCode(int statusCode) {
        return statusCode >= HttpURLConnection.HTTP_OK && statusCode < HttpURLConnection.HTTP_BAD_REQUEST;
    }

    public boolean isSuccess() {
        return failure == null;
    }

    public boolean isNotSuccess() {
        return failure != null;
    }

    public boolean hasResponseBody() {
        return Strings.isNotBlank(responseBody);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private int getErrorCode() {
        try {
            final JSONObject errorJson = new JSONObject(responseBody);
            if (errorJson.has(PUBLIC_API_ERROR_KEY)) {
                return errorJson.getInt(PUBLIC_API_ERROR_KEY);
            } else {
                return Consts.NOT_SET;
            }
        } catch (JSONException e) {
            return Consts.NOT_SET;
        }
    }

    private String getErrorKey() {
        try {
            final JSONObject errorJson = new JSONObject(responseBody);
            if (errorJson.has(PUBLIC_API_ERRORS_KEY)) {
                // public API error response
                final JSONArray errors = errorJson.getJSONArray(PUBLIC_API_ERRORS_KEY);
                return errors.getJSONObject(0).getString(PUBLIC_API_ERROR_MESSAGE_KEY);
            } else {
                // api-mobile errors keys
                return errorJson.getString(BAD_REQUEST_ERROR_KEY);
            }
        } catch (JSONException e) {
            return ApiRequestException.ERROR_KEY_NONE;
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                          .add("statusCode", statusCode)
                          .add("failure", failure).toString();
    }
}
