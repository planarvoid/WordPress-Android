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
import static com.soundcloud.android.api.ApiRequestException.Reason.VALIDATION_ERROR;

import com.soundcloud.android.Consts;
import org.apache.http.HttpStatus;

import java.io.IOException;

public final class ApiRequestException extends Exception {

    public static final String ERROR_KEY_NONE = "unknown";

    private final Reason errorReason;
    private final String errorKey;
    private final int errorCode;

    public enum Reason {
        AUTH_ERROR,
        NETWORK_ERROR,
        NOT_FOUND,
        NOT_ALLOWED,
        RATE_LIMITED,
        UNEXPECTED_RESPONSE,
        BAD_REQUEST,
        VALIDATION_ERROR,
        MALFORMED_INPUT,
        SERVER_ERROR
    }

    public static ApiRequestException unexpectedResponse(ApiRequest request, ApiResponse response) {
        int statusCode = response.getStatusCode();
        final boolean isValidStatusCode = statusCode < HttpStatus.SC_OK || statusCode < HttpStatus.SC_INTERNAL_SERVER_ERROR && statusCode >= HttpStatus.SC_BAD_REQUEST;
        checkArgument(isValidStatusCode, "Status code must be< 200 or between 400 and 500");
        return new ApiRequestException(UNEXPECTED_RESPONSE, request, response, "HTTP " + statusCode);
    }

    public static ApiRequestException badRequest(ApiRequest request, ApiResponse response, String errorKey) {
        return new ApiRequestException(BAD_REQUEST, request, response, errorKey);
    }

    public static ApiRequestException networkError(ApiRequest request, IOException ioException) {
        return new ApiRequestException(NETWORK_ERROR, request, ioException);
    }

    public static ApiRequestException notAllowed(ApiRequest request, ApiResponse response) {
        return new ApiRequestException(NOT_ALLOWED, request, response);
    }

    public static ApiRequestException notFound(ApiRequest request, ApiResponse response) {
        return new ApiRequestException(NOT_FOUND, request, response);
    }

    public static ApiRequestException rateLimited(ApiRequest request, ApiResponse response) {
        return new ApiRequestException(RATE_LIMITED, request, response);
    }

    public static ApiRequestException authError(ApiRequest request, ApiResponse response) {
        return new ApiRequestException(AUTH_ERROR, request, response);
    }

    public static ApiRequestException malformedInput(ApiRequest request, ApiMapperException e) {
        return new ApiRequestException(MALFORMED_INPUT, request, e);
    }

    public static ApiRequestException validationError(ApiRequest request, ApiResponse response, String errorKey, int errorCode) {
        return new ApiRequestException(VALIDATION_ERROR, request, response, errorKey, errorCode);
    }

    public static ApiRequestException serverError(ApiRequest request, ApiResponse response) {
        return new ApiRequestException(SERVER_ERROR, request, response);
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, ApiResponse response) {
        super("Request failed with reason " + errorReason + "; request = " + request + "; body = " + (response == null ? "<no response>" : response.getResponseBody()));
        this.errorReason = errorReason;
        this.errorKey = ERROR_KEY_NONE;
        this.errorCode = Consts.NOT_SET;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, Exception e) {
        super("Request failed with reason " + errorReason + "; request = " + request, e);
        this.errorReason = errorReason;
        this.errorKey = ERROR_KEY_NONE;
        this.errorCode = Consts.NOT_SET;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, ApiResponse response, String errorKey) {
        super("Request failed with reason " + errorReason + "; errorKey = " + errorKey + "; request = " + request + "; body = " + (response == null ? "<no response>" : response.getResponseBody()));
        this.errorReason = errorReason;
        this.errorKey = errorKey;
        this.errorCode = Consts.NOT_SET;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, ApiResponse response, String errorKey, int errorCode) {
        super("Request failed with reason " + errorReason + "; errorKey = " + errorKey + "; errorCode = " + errorCode + "; request = " + request + "; body = " + (response == null ? "<no response>" : response.getResponseBody()));
        this.errorReason = errorReason;
        this.errorKey = errorKey;
        this.errorCode = errorCode;
    }

    public Reason reason() {
        return errorReason;
    }

    public String errorKey() {
        return errorKey;
    }

    public int errorCode() {
        return errorCode;
    }

    public boolean isNetworkError() {
        return errorReason == NETWORK_ERROR;
    }

    public boolean loggable() {
        return errorReason == UNEXPECTED_RESPONSE
                || errorReason == MALFORMED_INPUT
                || errorReason == RATE_LIMITED
                || errorReason == SERVER_ERROR;
    }
}
