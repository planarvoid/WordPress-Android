package com.soundcloud.android.playback.streaming;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.CloudAPI;
import com.soundcloud.api.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Bundle;

import java.io.IOException;

@RunWith(DefaultTestRunner.class)
public class HeadTaskTest {
    private HeadTask task;
    private @Mock StreamItem streamItem;
    private @Mock
    PublicCloudAPI api;

    @Before
    public void before() {
        initMocks(this);
        task = new HeadTask(streamItem, api, false);
    }

    @Test
    public void shouldInitializeStreamSuccessCase() throws Exception {
        Stream stream = mock(Stream.class);
        when(api.resolveStreamUrl(anyString(), eq(false))).thenReturn(stream);
        Bundle b = task.execute();
        verify(streamItem).initializeFromStream(stream);
        expect(b.getBoolean("success")).toBeTrue();
    }

    @Test
    public void shouldMarkItemAsUnavailableIf4XXErrorStatus() throws Exception {
        CloudAPI.ResolverException exception = mock(CloudAPI.ResolverException.class);
        when(streamItem.streamItemUrl()).thenReturn("abc");
        when(exception.getStatusCode()).thenReturn(403);
        when(api.resolveStreamUrl("abc", false)).thenThrow(exception);
        when(streamItem.markUnavailable(403)).thenReturn(true);

        Bundle b = task.execute();
        expect(b.getInt("status")).toEqual(403);
    }

    @Test(expected = IOException.class)
    public void shouldMarkItemAsErrorIf5XXErrorStatus() throws Exception {
        CloudAPI.ResolverException exception = mock(CloudAPI.ResolverException.class);
        when(exception.getStatusCode()).thenReturn(501);
        when(api.resolveStreamUrl(anyString(), eq(false))).thenThrow(exception);
        when(streamItem.markUnavailable(501)).thenReturn(false);
        task.execute();
    }
}
