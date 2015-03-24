package com.soundcloud.android.api;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observer;

@RunWith(SoundCloudTestRunner.class)
public class ApiClientRxTest {

    private ApiClientRx apiClientRx;

    @Mock ApiClient client;
    @Mock ApiRequest request;
    @Mock Observer observer;

    @Before
    public void setup() {
        apiClientRx = new ApiClientRx(client);
    }

    @Test
    public void shouldEmitResponseOnSuccess() {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.response(request).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(response);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitExceptionOnFailedResponse() {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.response(request).subscribe(observer);

        verify(observer).onError(isA(ApiRequestException.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }

    @Test
    public void shouldEmitMappedResponseOnSuccess() throws Exception {
        ApiTrack track = new ApiTrack();
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(request, response)).thenReturn(track);

        apiClientRx.mappedResponse(request).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(track);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitExceptionIfResponseFailedBeforeMapping() throws Exception {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        apiClientRx.mappedResponse(request).subscribe(observer);

        verify(observer).onError(isA(ApiRequestException.class));
        verify(observer, never()).onNext(any());
        verify(observer, never()).onCompleted();
    }

    @Test
    public void shouldEmitExceptionOnFailedMapping() throws Exception {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(request, response)).thenThrow(new ApiMapperException("fail"));

        apiClientRx.mappedResponse(request).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onError(isA(ApiMapperException.class));
        inOrder.verifyNoMoreInteractions();
    }
}