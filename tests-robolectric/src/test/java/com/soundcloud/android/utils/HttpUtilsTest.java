package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.api.legacy.Request;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
public class HttpUtilsTest {

    @Test
    public void testAddQueryParams() {
        Request request = Request.to("/resource");
        HttpUtils.addQueryParams(request, "a", "1", "b", "2");
        expect(request.queryString()).toEqual("a=1&b=2");
    }

    @Test
    public void addQueryParamsShouldDoNothingForBlankParams() {
        Request request = Request.to("/resource");

        HttpUtils.addQueryParams(request);
        expect(request.queryString()).toEqual(Request.to("/resource").queryString());

        HttpUtils.addQueryParams(request, null);
        expect(request.queryString()).toEqual(Request.to("/resource").queryString());

        HttpUtils.addQueryParams(request, new String[]{});
        expect(request.queryString()).toEqual(Request.to("/resource").queryString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void addQueryParamsExpectsArgumentPairs() {
        Request request = Request.to("/resource");
        HttpUtils.addQueryParams(request, "a");
    }
}
