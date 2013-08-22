package com.soundcloud.android.streaming;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.AndroidCloudAPI;
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
    private @Mock AndroidCloudAPI api;

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

        Bundle b = task.execute();

        verify(streamItem).markUnavailable(403);
        expect(b.getInt("status")).toEqual(403);
    }

    @Test
    public void shouldMarkItemAsErrorIf5XXErrorStatus() throws Exception {
        CloudAPI.ResolverException exception = mock(CloudAPI.ResolverException.class);
        when(exception.getStatusCode()).thenReturn(501);
        when(api.resolveStreamUrl(anyString(), eq(false))).thenThrow(exception);

        try {
            task.execute();
            fail("expected IO exception");
        } catch (IOException e) {

        }
        verify(streamItem).setHttpError(501);
    }
}
