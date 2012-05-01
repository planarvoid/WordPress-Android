package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class AppTest {
    @Test
    public void shouldHaveProductionEnabled() throws Exception {
        // make sure this doesn't get accidentally committed
        expect(SoundCloudApplication.API_PRODUCTION).toBeTrue();
    }

    @Test
    public void shouldNotBeDetectedAsDalvik() throws Exception {
        expect(SoundCloudApplication.DALVIK).toBeFalse();
    }
}
