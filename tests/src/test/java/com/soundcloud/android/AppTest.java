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

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        // live
        expect(DefaultTestRunner.application.getClientSecret(true))
                .toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");

        // sandbox
        expect(DefaultTestRunner.application.getClientSecret(false))
                .toEqual("0000000pGDzQNAPHzBH6hBTHphl4Q1e9");
    }
}
