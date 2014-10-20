package com.soundcloud.android.api;


import com.google.common.base.Objects;
import com.soundcloud.android.Consts;
import com.soundcloud.android.utils.ScTextUtils;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;

public class ApiResponse {
    private static final int SC_REQUEST_TOO_MANY_REQUESTS = 429;

    private final int statusCode;
    private final String responseBody;
    @Nullable private ApiRequestException failure;

    protected ApiResponse(ApiRequest request, int statusCode, String responseBody) {
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
        } else if (!isSuccessCode(statusCode)) {
            failure = ApiRequestException.unexpectedResponse(request);
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
        return ScTextUtils.isNotBlank(responseBody.trim());
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("statusCode", statusCode)
                .add("failure", failure).toString();
    }
}
