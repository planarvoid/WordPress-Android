package com.soundcloud.android.api;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observer;
import rx.schedulers.Schedulers;

@RunWith(SoundCloudTestRunner.class)
public class ApiSchedulerTest {

    private ApiScheduler scheduler;

    @Mock ApiClient client;
    @Mock ApiRequest request;
    @Mock Observer observer;

    @Before
    public void setup() {
        scheduler = new ApiScheduler(client, Schedulers.immediate());
    }

    @Test
    public void shouldEmitMappedResponseOnSuccess() throws Exception {
        ApiTrack track = new ApiTrack();
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(request, response)).thenReturn(track);

        scheduler.mappedResponse(request).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(track);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitExceptionOnFailedResponse() throws Exception {
        ApiResponse response = new ApiResponse(request, 500, "");
        when(client.fetchResponse(request)).thenReturn(response);

        scheduler.mappedResponse(request).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onError(isA(ApiRequestException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitExceptionOnFailedMapping() throws Exception {
        ApiResponse response = new ApiResponse(request, 200, "");
        when(client.fetchResponse(request)).thenReturn(response);
        when(client.mapResponse(request, response)).thenThrow(new ApiMapperException("fail"));

        scheduler.mappedResponse(request).subscribe(observer);

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onError(isA(ApiMapperException.class));
        inOrder.verifyNoMoreInteractions();
    }
}