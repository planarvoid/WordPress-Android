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

    @Test
    public void shouldUrlEncodePathSpecialCharactersParametersOnPath() {
        String resolvedPath = APIEndpoints.RELATED_TRACKS.path(" %{}±!@#$%^&*()_+}{<>?");
        expect(resolvedPath).toEqual("/tracks/+%25%7B%7D%C2%B1%21%40%23%24%25%5E%26*%28%29_%2B%7D%7B%3C%3E%3F/related");
    }
}
