package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class CheckoutStartedTest {

    @Test
    public void mapsCheckoutResultToTokenString() {
        CheckoutStarted result = new CheckoutStarted("token_123");
        expect(CheckoutStarted.TOKEN.call(result)).toEqual("token_123");
    }

}