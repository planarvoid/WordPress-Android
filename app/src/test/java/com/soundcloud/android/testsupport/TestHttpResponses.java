package com.soundcloud.android.testsupport;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;

public final class TestHttpResponses {

    public static Response.Builder response(int code) throws IOException {
        return response(code,
                ResponseBody.create(MediaType.parse("text"), "<test response body>"));
    }

    public static Response.Builder response(int code, ResponseBody responseBody) throws IOException {
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .request(TestHttpRequests.stub())
                .body(responseBody);
    }

    public static Response.Builder jsonResponse(int code, String jsonBody) throws IOException {
        return new Response.Builder()
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .request(TestHttpRequests.stub())
                .body(ResponseBody.create(MediaType.parse("application/json"), jsonBody));
    }


}
