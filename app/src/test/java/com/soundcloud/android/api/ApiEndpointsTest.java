package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ApiEndpointsTest {

    @Test
    public void shouldResolvePathFromPathParameters() {
        String resolvedPath = ApiEndpoints.RELATED_TRACKS.path("1");
        expect(resolvedPath).toEqual("/tracks/1/related");
    }

    @Test
    public void shouldPercentEncodePathParameters() {
        String resolvedPath = ApiEndpoints.RELATED_TRACKS.path("has space");
        expect(resolvedPath).toEqual("/tracks/has%20space/related");
    }

}
