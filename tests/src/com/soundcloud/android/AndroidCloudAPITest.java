package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.api.Env;
import com.soundcloud.api.Token;
import org.junit.Test;

public class AndroidCloudAPITest {

    @Test
    public void testAddTokenToUrl() throws Exception {
        AndroidCloudAPI api = new AndroidCloudAPI.Wrapper(null, null, null, null,
                new Token("foo", "bar"), Env.LIVE);

        expect(api.addTokenToUrl("http://foo.com?bla=baz&foo=1")).toEqual("http://foo.com?bla=baz&foo=1&oauth_token=foo");
        expect(api.addTokenToUrl("http://foo.com")).toEqual("http://foo.com?oauth_token=foo");
    }

}
