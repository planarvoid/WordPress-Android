package com.soundcloud.android.api.http;

import static com.soundcloud.android.api.http.APIRequestException.APIErrorReason.BAD_RESPONSE;
import static com.soundcloud.android.api.http.APIRequestException.APIErrorReason.NETWORK_COMM_ERROR;
import static com.soundcloud.android.api.http.APIRequestException.APIErrorReason.RATE_LIMITED;
import static com.soundcloud.android.api.http.APIRequestException.APIErrorReason.TOKEN_AUTH_ERROR;

import com.google.common.base.Objects;
import com.soundcloud.api.CloudAPI;

import java.io.IOException;

public class APIRequestException extends RuntimeException {

    private APIResponse mResponse;
    private APIRequest mRequest;

    private APIErrorReason errorReason;

    public enum APIErrorReason {
        TOKEN_AUTH_ERROR,
        NETWORK_COMM_ERROR,
        BAD_RESPONSE,
        RATE_LIMITED,
        UNKNOWN_ERROR
    }

    public static APIRequestException badResponse(APIRequest request, APIResponse response) {
        return new APIRequestException(BAD_RESPONSE, request, response, (Exception)null);
    }

    public static APIRequestException badResponse(APIRequest request, APIResponse response,Exception exception) {
        return new APIRequestException(BAD_RESPONSE, request, response, exception);
    }

    public static APIRequestException badResponse(APIRequest request, APIResponse response, String msg) {
        return new APIRequestException(BAD_RESPONSE, request, response, msg);
    }

    public static APIRequestException networkCommsError(APIRequest request, IOException ioException) {
        return new APIRequestException(NETWORK_COMM_ERROR, request, null, ioException);
    }

    public static APIRequestException rateLimited(APIRequest request, APIResponse response) {
        return new APIRequestException(RATE_LIMITED, request, response, (Exception)null);
    }


    public static APIRequestException authError(APIRequest request, CloudAPI.InvalidTokenException e) {
        return new APIRequestException(TOKEN_AUTH_ERROR,  request, null, e);
    }


    private APIRequestException(APIErrorReason errorReason, APIRequest request, APIResponse response, Exception e) {
        super(e);
        this.errorReason = errorReason;
        this.mRequest = request;
        this.mResponse = response;
    }

    private APIRequestException(APIErrorReason errorReason, APIRequest request, APIResponse response, String msg) {
        super(msg);
        this.errorReason = errorReason;
        this.mRequest = request;
        this.mResponse = response;
    }

    public APIErrorReason reason() {
        return errorReason;
    }

    public APIResponse response() {
        return mResponse;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this).omitNullValues()
                .add("errorReason", errorReason)
                .add("exceptionMessage", getMessage())
                .add("mRequest", mRequest)
                .add("mResponse", mResponse).toString();
    }
}