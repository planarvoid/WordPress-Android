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
        // live
        expect(AndroidCloudAPI.Wrapper.getClientSecret(Env.LIVE))
                .toEqual("26a5240f7ee0ee2d4fa9956ed80616c2");

        // sandbox
        expect(AndroidCloudAPI.Wrapper.getClientSecret(Env.SANDBOX))
                .toEqual("0000000pGDzQNAPHzBH6hBTHphl4Q1e9");
    }
}
