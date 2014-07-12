package com.soundcloud.android.api;

import static com.soundcloud.android.api.APIRequestException.APIErrorReason.BAD_RESPONSE;
import static com.soundcloud.android.api.APIRequestException.APIErrorReason.NETWORK_COMM_ERROR;
import static com.soundcloud.android.api.APIRequestException.APIErrorReason.RATE_LIMITED;
import static com.soundcloud.android.api.APIRequestException.APIErrorReason.TOKEN_AUTH_ERROR;

import com.google.common.base.Objects;
import com.soundcloud.api.CloudAPI;

import java.io.IOException;

public class APIRequestException extends Exception {

    private final APIResponse response;
    private final APIRequest request;
    private final APIErrorReason errorReason;

    public enum APIErrorReason {
        TOKEN_AUTH_ERROR,
        NETWORK_COMM_ERROR,
        BAD_RESPONSE,
        RATE_LIMITED,
        UNKNOWN_ERROR
    }

    public static APIRequestException badResponse(APIRequest request, APIResponse response) {
        return new APIRequestException(BAD_RESPONSE, request, response, (Exception) null);
    }

    public static APIRequestException badResponse(APIRequest request, APIResponse response, Exception exception) {
        return new APIRequestException(BAD_RESPONSE, request, response, exception);
    }

    public static APIRequestException badResponse(APIRequest request, APIResponse response, String msg) {
        return new APIRequestException(BAD_RESPONSE, request, response, msg);
    }

    public static APIRequestException networkCommsError(APIRequest request, IOException ioException) {
        return new APIRequestException(NETWORK_COMM_ERROR, request, null, ioException);
    }

    public static APIRequestException rateLimited(APIRequest request, APIResponse response) {
        return new APIRequestException(RATE_LIMITED, request, response, (Exception) null);
    }

    public static APIRequestException authError(APIRequest request, CloudAPI.InvalidTokenException e) {
        return new APIRequestException(TOKEN_AUTH_ERROR, request, null, e);
    }


    private APIRequestException(APIErrorReason errorReason, APIRequest request, APIResponse response, Exception e) {
        super(e);
        this.errorReason = errorReason;
        this.request = request;
        this.response = response;
    }

    private APIRequestException(APIErrorReason errorReason, APIRequest request, APIResponse response, String msg) {
        super(msg);
        this.errorReason = errorReason;
        this.request = request;
        this.response = response;
    }

    public APIErrorReason reason() {
        return errorReason;
    }

    public APIResponse response() {
        return response;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
                .add("errorReason", errorReason)
                .add("exceptionMessage", getMessage())
                .add("mRequest", request)
                .add("mResponse", response).toString();
    }
}