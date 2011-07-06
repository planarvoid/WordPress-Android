package com.soundcloud.android;

import com.soundcloud.api.Env;
import com.soundcloud.api.Token;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.soundcloud.android.robolectric.DefaultTestRunner;

import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class AndroidCloudAPITest {

    @Test
    public void testAddTokenToUrl() throws Exception {
        AndroidCloudAPI api = new AndroidCloudAPI.Wrapper(null, null, null, null,
                new Token("foo", "bar"), Env.LIVE);

        assertThat(api.addTokenToUrl("http://foo.com?bla=baz&foo=1"),
                equalTo("http://foo.com?bla=baz&foo=1&oauth_token=foo"));

        assertThat(api.addTokenToUrl("http://foo.com"),
                equalTo("http://foo.com?oauth_token=foo"));
    }
}
