package com.soundcloud.android;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.api.Env;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(DefaultTestRunner.class)
public class AppTest {
    @Test
    public void shouldNotBeDetectedAsDalvik() throws Exception {
        expect(SoundCloudApplication.DALVIK).toBeFalse();
    }

    @Test
    public void shouldDeobfuscateClientSecret() throws Exception {
        expect(AndroidCloudAPI.Wrapper.getClientSecret(Env.SANDBOX))
                .toEqual("0000000pGDzQNAPHzBH6hBTHphl4Q1e9");

        expect(AndroidCloudAPI.Wrapper.getClientSecret(Env.LIVE))
                .toEqual("GANQKmfSMpx9FUJ7G837OQZzeBEyv7Fj3ART1WvjQA");
    }
}
