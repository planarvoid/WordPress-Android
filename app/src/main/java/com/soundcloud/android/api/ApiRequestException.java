package com.soundcloud.android.api;

import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_ALLOWED;
import static com.soundcloud.android.api.ApiRequestException.Reason.UNEXPECTED_RESPONSE;
import static com.soundcloud.android.api.ApiRequestException.Reason.MALFORMED_INPUT;
import static com.soundcloud.android.api.ApiRequestException.Reason.NETWORK_ERROR;
import static com.soundcloud.android.api.ApiRequestException.Reason.RATE_LIMITED;
import static com.soundcloud.android.api.ApiRequestException.Reason.NOT_FOUND;
import static com.soundcloud.android.api.ApiRequestException.Reason.AUTH_ERROR;

import com.google.common.base.Objects;
import com.soundcloud.api.CloudAPI;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class ApiRequestException extends Exception {

    private final ApiRequest request;
    private final Reason errorReason;

    public enum Reason {
        AUTH_ERROR,
        NETWORK_ERROR,
        NOT_FOUND,
        NOT_ALLOWED,
        RATE_LIMITED,
        UNEXPECTED_RESPONSE,
        MALFORMED_INPUT
    }

    public static ApiRequestException unexpectedResponse(ApiRequest request) {
        return new ApiRequestException(UNEXPECTED_RESPONSE, request, (Exception) null);
    }

    public static ApiRequestException unexpectedResponse(ApiRequest request, Exception exception) {
        return new ApiRequestException(UNEXPECTED_RESPONSE, request, exception);
    }

    public static ApiRequestException unexpectedResponse(ApiRequest request, String msg) {
        return new ApiRequestException(UNEXPECTED_RESPONSE, request, msg);
    }

    public static ApiRequestException networkError(ApiRequest request, IOException ioException) {
        return new ApiRequestException(NETWORK_ERROR, request, ioException);
    }

    public static ApiRequestException notAllowed(ApiRequest request) {
        return new ApiRequestException(NOT_ALLOWED, request, (Exception) null);
    }

    public static ApiRequestException notFound(ApiRequest request) {
        return new ApiRequestException(NOT_FOUND, request, (Exception) null);
    }

    public static ApiRequestException rateLimited(ApiRequest request) {
        return new ApiRequestException(RATE_LIMITED, request, (Exception) null);
    }

    public static ApiRequestException authError(ApiRequest request, CloudAPI.InvalidTokenException e) {
        return new ApiRequestException(AUTH_ERROR, request, e);
    }

    public static ApiRequestException malformedInput(ApiRequest request, ApiMapperException e) {
        return new ApiRequestException(MALFORMED_INPUT, request, e);
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, @Nullable Exception e) {
        super(e);
        this.errorReason = errorReason;
        this.request = request;
    }

    private ApiRequestException(Reason errorReason, ApiRequest request, String msg) {
        super(msg);
        this.errorReason = errorReason;
        this.request = request;
    }

    public Reason reason() {
        return errorReason;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
                .add("errorReason", errorReason)
                .add("exceptionMessage", getMessage())
                .add("request", request).toString();
    }
}