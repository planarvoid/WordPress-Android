package com.soundcloud.android.api;

import static org.mockito.Mockito.when;

import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class ApiClientRxV2Test {
    private ApiClientRxV2 apiClientRx;

    @Mock private ApiClient client;
    @Mock private ApiRequest request;

    @Before
    public void setup() {
        apiClientRx = new ApiClientRxV2(client);
    }

    @Test
    public void shouldEmitResponseOnSuccess() {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.response(request).test()
                   .assertValue(response)
                   .assertNoErrors()
                   .assertComplete();
    }

    @Test
    public void shouldEmitErrorOnFailedResponse() {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.response(request).test()
                   .assertError(ApiRequestException.class)
                   .assertNoValues()
                   .assertNotComplete();
    }

    @Test
    public void shouldEmitOnCompleteWhenIgnoringResult() {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.ignoreResultRequest(request).test()
                   .assertComplete()
                   .assertNoValues()
                   .assertNoErrors();
    }

    @Test
    public void shouldEmitOnErrorWhenIgnoringResult() {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.ignoreResultRequest(request).test()
                   .assertError(ApiRequestException.class)
                   .assertNoValues()
                   .assertNotComplete();
    }

    @Test
    public void shouldEmitMappedResponseOnSuccess() throws Exception {
        String mappedResponse = "mapped";
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(response, TypeToken.of(String.class))).thenReturn(mappedResponse);

        apiClientRx.mappedResponse(request, String.class).test()
                   .assertValue(mappedResponse)
                   .assertNoErrors()
                   .assertComplete();
    }

    @Test
    public void shouldEmitExceptionIfResponseFailedBeforeMapping() throws Exception {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.mappedResponse(request, String.class).test()
                   .assertError(ApiRequestException.class)
                   .assertNoValues()
                   .assertNotComplete();
    }

    @Test
    public void shouldEmitExceptionOnFailedMapping() throws Exception {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(response, TypeToken.of(String.class))).thenThrow(new ApiMapperException("fail"));

        apiClientRx.mappedResponse(request, String.class).test()
                   .assertError(ApiMapperException.class)
                   .assertNoValues()
                   .assertNotComplete();
    }

    @Test
    public void shouldHandleThrowingClient() throws Exception {
        when(client.fetchResponse(request)).thenThrow(IOException.class);

        apiClientRx.response(request).test()
                   .assertError(IOException.class)
                   .assertNoValues()
                   .assertNotComplete();
    }
}
