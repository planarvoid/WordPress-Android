package com.soundcloud.android;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class AppTests {

    @Test @SuppressWarnings({"ConstantConditions"})
    public void shouldHaveProductionEnabled() throws Exception {
        // make sure this doesn't get accidentally committed
        assertThat(SoundCloudApplication.API_PRODUCTION, is(true));
    }

    @Test
    public void shouldSignUrlWithAccessToken() throws Exception {
        SoundCloudApplication app =
                (SoundCloudApplication) Robolectric.application;
        app.onCreate();
        assertThat(app.signUrl("http://foo.com"),
                   equalTo("http://foo.com?oauth_token=null"));
        assertThat(app.signUrl("http://foo.com?bla=blubb"),
                   equalTo("http://foo.com?bla=blubb&oauth_token=null"));
    }
}
