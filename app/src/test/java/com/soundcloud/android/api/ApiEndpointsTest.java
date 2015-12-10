package com.soundcloud.android.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class ApiEndpointsTest extends AndroidUnitTest {

    @Test
    public void shouldResolvePathFromPathParameters() {
        String resolvedPath = ApiEndpoints.RELATED_TRACKS.path("1");
        assertThat(resolvedPath).isEqualTo("/tracks/1/related");
    }

    @Test
    public void shouldPercentEncodePathParameters() {
        String resolvedPath = ApiEndpoints.RELATED_TRACKS.path("has space");
        assertThat(resolvedPath).isEqualTo("/tracks/has%20space/related");
    }
}
