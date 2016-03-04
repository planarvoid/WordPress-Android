package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.utils.BuildHelper;
import com.soundcloud.android.utils.DeviceHelper;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class ApiKillSwitchInterceptorTest {

    private TestEventBus eventBus = new TestEventBus();
    private ApiKillSwitchInterceptor interceptor;

    @Mock private BuildHelper buildHelper;
    @Mock private DeviceHelper deviceHelper;
    @Mock private Interceptor.Chain chain;
    private Response.Builder builder;

    @Before
    public void setUp() throws Exception {
        interceptor = new ApiKillSwitchInterceptor(eventBus, buildHelper, deviceHelper);
        Request request = new Request.Builder().url("http://boo").build();
        builder = new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200);
    }

    @Test
    public void shouldPublishForceUpdateEventWhenHeaderPresent() throws IOException {
        final Response response = builder
                .header(ApiKillSwitchInterceptor.KILL_SWITCH_HEADER, "true")
                .build();
        when(chain.proceed(any(Request.class))).thenReturn(response);

        interceptor.intercept(chain);

        assertThat(eventBus.lastEventOn(EventQueue.FORCE_UPDATE)).isNotNull();
    }

    @Test
    public void shouldNotPublishForceUpdateEventWhenHeaderCannotBeInterpreted() throws IOException {
        final Response response = builder
                .header(ApiKillSwitchInterceptor.KILL_SWITCH_HEADER, "wat z4t")
                .build();
        when(chain.proceed(any(Request.class))).thenReturn(response);

        interceptor.intercept(chain);

        eventBus.verifyNoEventsOn(EventQueue.FORCE_UPDATE);
    }

    @Test
    public void shouldNotPublishForceUpdateEventWhenHeaderAbsent() throws IOException {
        final Response response = builder
                .build();
        when(chain.proceed(any(Request.class))).thenReturn(response);

        interceptor.intercept(chain);

        eventBus.verifyNoEventsOn(EventQueue.FORCE_UPDATE);
    }
}
