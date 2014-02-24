package com.soundcloud.android.api;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

public class APIEndpointsTest {

    @Test
    public void shouldResolvePathFromPathParameters() {
        String resolvedPath = APIEndpoints.RELATED_TRACKS.path("1");
        expect(resolvedPath).toEqual("/tracks/1/related");
    }

    @Test
    public void shouldUrlEncodePathParameters() {
        String resolvedPath = APIEndpoints.RELATED_TRACKS.path("has space");
        expect(resolvedPath).toEqual("/tracks/has+space/related");
    }
}
