package com.soundcloud.android.utils;


import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class HttpUtilsTest extends AndroidUnitTest {

    @Test
    public void testAddQueryParams() {
        Request request = Request.to("/resource");
        HttpUtils.addQueryParams(request, "a", "1", "b", "2");
        assertThat(request.queryString()).isEqualTo("a=1&b=2");
    }

    @Test
    public void addQueryParamsShouldDoNothingForBlankParams() {
        Request request = Request.to("/resource");

        HttpUtils.addQueryParams(request);
        assertThat(request.queryString()).isEqualTo(Request.to("/resource").queryString());

        HttpUtils.addQueryParams(request);
        assertThat(request.queryString()).isEqualTo(Request.to("/resource").queryString());

        HttpUtils.addQueryParams(request, new String[]{});
        assertThat(request.queryString()).isEqualTo(Request.to("/resource").queryString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addQueryParamsExpectsArgumentPairs() {
        Request request = Request.to("/resource");
        HttpUtils.addQueryParams(request, "a");
    }
}
