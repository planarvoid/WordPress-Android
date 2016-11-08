package com.soundcloud.android.testsupport;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;

public final class TestHttpResponses {

    public static Response.Builder response(int code) throws IOException {
        return response(code,
                        ResponseBody.create(MediaType.parse("text"), "<test response body>"));
    }

    public static Response.Builder response(int code, ResponseBody responseBody) {
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .request(TestHttpRequests.stub())
                .body(responseBody);
    }

    public static Response.Builder jsonResponse(int code, String jsonBody) {
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .request(TestHttpRequests.stub())
                .body(ResponseBody.create(MediaType.parse("application/json"), jsonBody));
    }


}
