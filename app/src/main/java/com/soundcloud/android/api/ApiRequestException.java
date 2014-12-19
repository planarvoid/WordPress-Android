package com.soundcloud.android.api;

import static com.soundcloud.android.api.ApiRequestException.Reason.BAD_REQUEST;
import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_ALLOWED;
import static com.soundcloud.android.api.ApiRequestException.Reason.UNEXPECTED_RESPONSE;
import static com.soundcloud.android.api.ApiRequestException.Reason.MALFORMED_INPUT;
import static com.soundcloud.android.api.ApiRequestException.Reason.NETWORK_ERROR;
import static com.soundcloud.android.api.ApiRequestException.Reason.RATE_LIMITED;
import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_FOUND;
import static com.soundcloud.android.api.ApiRequestException.Reason.AUTH_ERROR;

import com.google.common.base.Objects;
import com.soundcloud.api.CloudAPI;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public final class ApiRequestException extends Exception {

    public static final String ERROR_KEY_NONE = "unknown";

    private final ApiRequest request;
    private final Reason errorReason;
    private final String errorKey;

    public static final List<Reason> LOGGABLE_REASONS =
            Arrays.asList(UNEXPECTED_RESPONSE, MALFORMED_INPUT, RATE_LIMITED);
    public enum Reason {
        AUTH_ERROR,
        NETWORK_ERROR,
        NOT_FOUND,
        NOT_ALLOWED,
        RATE_LIMITED,
        UNEXPECTED_RESPONSE,
        BAD_REQUEST,
        MALFORMED_INPUT
    }

    public static ApiRequestException unexpectedResponse(ApiRequest request) {
        return new ApiRequestException(UNEXPECTED_RESPONSE, request);
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

    private ApiRequestException(Reason errorReason, ApiRequest request) {
        this.errorReason = errorReason;
        this.request = request;
        this.errorKey = ERROR_KEY_NONE;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, Exception e) {
        super(e);
        this.errorReason = errorReason;
        this.request = request;
        this.errorKey = ERROR_KEY_NONE;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, String errorKey) {
        this.errorReason = errorReason;
        this.request = request;
        this.errorKey = errorKey;
    }

    public Reason reason() {
        return errorReason;
    }

    public String errorKey() {
        return errorKey;
    }

    public boolean loggable() {
        return LOGGABLE_REASONS.contains(reason());
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
                .add("errorReason", errorReason)
                .add("exceptionMessage", getMessage())
                .add("request", request).toString();
    }
}
