package com.soundcloud.android.api;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.ScTextUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

public class ApiResponse {

    private static final String BAD_REQUEST_ERROR_KEY = "error_key";
    private static final int SC_REQUEST_TOO_MANY_REQUESTS = 429;

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
        if (statusCode == SC_REQUEST_TOO_MANY_REQUESTS) {
            failure = ApiRequestException.rateLimited(request);
        } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
            failure = ApiRequestException.notFound(request);
        } else if (statusCode == HttpStatus.SC_FORBIDDEN) {
            failure = ApiRequestException.notAllowed(request);
        } else if (statusCode == HttpStatus.SC_BAD_REQUEST) {
            failure = ApiRequestException.badRequest(request, getErrorKey());
        } else if (statusCode >= HttpStatus.SC_INTERNAL_SERVER_ERROR) {
            failure = ApiRequestException.serverError(request);
        } else if (!isSuccessCode(statusCode)) {
            failure = ApiRequestException.unexpectedResponse(request, statusCode);
        }
    }

    protected ApiResponse(ApiRequestException failure) {
        this.statusCode = Consts.NOT_SET;
        this.responseBody = ScTextUtils.EMPTY_STRING;
        this.failure = failure;
    }

    @Nullable
    public ApiRequestException getFailure() {
        return failure;
    }

    private boolean isSuccessCode(int statusCode) {
        return statusCode >= HttpStatus.SC_OK && statusCode < HttpStatus.SC_BAD_REQUEST;
    }

    public boolean isSuccess() {
        return failure == null;
    }

    public boolean isNotSuccess() {
        return failure != null;
    }

    public boolean hasResponseBody() {
        return ScTextUtils.isNotBlank(responseBody);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    private String getErrorKey() {
        try {
            return new JSONObject(responseBody).getString(BAD_REQUEST_ERROR_KEY);
        } catch (JSONException e) {
            return ApiRequestException.ERROR_KEY_NONE;
        }
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("statusCode", statusCode)
                .add("failure", failure).toString();
    }
}
