package com.soundcloud.android;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class AppTest {
    @Test
    public void shouldHaveProductionEnabled() throws Exception {
        // make sure this doesn't get accidentally committed
        assertThat(SoundCloudApplication.API_PRODUCTION, is(true));
    }

    @Test
    public void shouldNotBeDetectedAsDalvik() throws Exception {
        assertThat(SoundCloudApplication.DALVIK, is(false));
    }
}
