package com.soundcloud.android.api;

import static com.google.common.base.Preconditions.checkArgument;
import static com.soundcloud.android.api.ApiRequestException.Reason.AUTH_ERROR;
import static com.soundcloud.android.api.ApiRequestException.Reason.BAD_REQUEST;
import static com.soundcloud.android.api.ApiRequestException.Reason.MALFORMED_INPUT;
import static com.soundcloud.android.api.ApiRequestException.Reason.NETWORK_ERROR;
import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_ALLOWED;
import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_FOUND;
import static com.soundcloud.android.api.ApiRequestException.Reason.RATE_LIMITED;
import static com.soundcloud.android.api.ApiRequestException.Reason.SERVER_ERROR;
import static com.soundcloud.android.api.ApiRequestException.Reason.UNEXPECTED_RESPONSE;

import com.soundcloud.api.CloudAPI;
import org.apache.http.HttpStatus;

import java.io.IOException;

public final class ApiRequestException extends Exception {

    public static final String ERROR_KEY_NONE = "unknown";

    private final Reason errorReason;
    private final String errorKey;

    public enum Reason {
        AUTH_ERROR,
        NETWORK_ERROR,
        NOT_FOUND,
        NOT_ALLOWED,
        RATE_LIMITED,
        UNEXPECTED_RESPONSE,
        BAD_REQUEST,
        MALFORMED_INPUT,
        SERVER_ERROR
    }

    public static ApiRequestException unexpectedResponse(ApiRequest request, int statusCode) {
        final boolean isValidStatusCode = statusCode < HttpStatus.SC_OK || (statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR && statusCode >= HttpStatus.SC_BAD_REQUEST);
        checkArgument(isValidStatusCode, "Status code must be< 200 or between 400 and 500");
        return new ApiRequestException(UNEXPECTED_RESPONSE, request, "HTTP " + statusCode);
    }

    public static ApiRequestException badRequest(ApiRequest request, String errorKey) {
        return new ApiRequestException(BAD_REQUEST, request, errorKey);
    }

    public static ApiRequestException networkError(ApiRequest request, IOException ioException) {
        return new ApiRequestException(NETWORK_ERROR, request, ioException);
    }

    public static ApiRequestException notAllowed(ApiRequest request) {
        return new ApiRequestException(NOT_ALLOWED, request);
    }

    public static ApiRequestException notFound(ApiRequest request) {
        return new ApiRequestException(NOT_FOUND, request);
    }

    public static ApiRequestException rateLimited(ApiRequest request) {
        return new ApiRequestException(RATE_LIMITED, request);
    }

    public static ApiRequestException authError(ApiRequest request, CloudAPI.InvalidTokenException e) {
        return new ApiRequestException(AUTH_ERROR, request, e);
    }

    public static ApiRequestException malformedInput(ApiRequest request, ApiMapperException e) {
        return new ApiRequestException(MALFORMED_INPUT, request, e);
    }

    public static ApiRequestException serverError(ApiRequest request) {
        return new ApiRequestException(SERVER_ERROR, request);
    }

    private ApiRequestException(Reason errorReason, ApiRequest request) {
        super("Request failed with reason " + errorReason + "; request = " + request);
        this.errorReason = errorReason;
        this.errorKey = ERROR_KEY_NONE;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, Exception e) {
        super("Request failed with reason " + errorReason + "; request = " + request, e);
        this.errorReason = errorReason;
        this.errorKey = ERROR_KEY_NONE;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, String errorKey) {
        super("Request failed with reason " + errorReason + "; errorKey = " + errorKey + "; request = " + request);
        this.errorReason = errorReason;
        this.errorKey = errorKey;
    }

    public Reason reason() {
        return errorReason;
    }

    public String errorKey() {
        return errorKey;
    }

    public boolean isNetworkError(){
        return errorReason == NETWORK_ERROR;
    }

    public boolean loggable() {
        return errorReason == UNEXPECTED_RESPONSE
                || errorReason == MALFORMED_INPUT
                || errorReason == RATE_LIMITED
                || errorReason == SERVER_ERROR;
    }
}
