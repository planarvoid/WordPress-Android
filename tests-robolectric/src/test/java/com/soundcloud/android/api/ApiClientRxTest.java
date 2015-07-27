package com.soundcloud.android.api;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class ApiClientRxTest {

    private ApiClientRx apiClientRx;

    @Mock private ApiClient client;
    @Mock private ApiRequest request;
    @Mock private Observer<ApiResponse> responseObserver;
    @Mock private Observer<String> stringObserver;

    @Before
    public void setup() {
        apiClientRx = new ApiClientRx(client);
    }

    @Test
    public void shouldEmitResponseOnSuccess() {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.response(request).subscribe(responseObserver);

        InOrder inOrder = inOrder(responseObserver);
        inOrder.verify(responseObserver).onNext(response);
        inOrder.verify(responseObserver).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitExceptionOnFailedResponse() {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.response(request).subscribe(responseObserver);

        verify(responseObserver).onError(isA(ApiRequestException.class));
        verify(responseObserver, never()).onNext(any(ApiResponse.class));
        verify(responseObserver, never()).onCompleted();
    }

    @Test
    public void shouldEmitMappedResponseOnSuccess() throws Exception {
        String mappedResponse = "mapped";
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(response, TypeToken.of(String.class))).thenReturn(mappedResponse);

        apiClientRx.mappedResponse(request, String.class).subscribe(stringObserver);

        InOrder inOrder = inOrder(stringObserver);
        inOrder.verify(stringObserver).onNext(mappedResponse);
        inOrder.verify(stringObserver).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitExceptionIfResponseFailedBeforeMapping() throws Exception {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.mappedResponse(request, String.class).subscribe(stringObserver);

        verify(stringObserver).onError(isA(ApiRequestException.class));
        verify(stringObserver, never()).onNext(anyString());
        verify(stringObserver, never()).onCompleted();
    }

    @Test
    public void shouldEmitExceptionOnFailedMapping() throws Exception {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(response, TypeToken.of(String.class))).thenThrow(new ApiMapperException("fail"));

        apiClientRx.mappedResponse(request, String.class).subscribe(stringObserver);

        InOrder inOrder = inOrder(stringObserver);
        inOrder.verify(stringObserver).onError(isA(ApiMapperException.class));
        inOrder.verifyNoMoreInteractions();
    }
}