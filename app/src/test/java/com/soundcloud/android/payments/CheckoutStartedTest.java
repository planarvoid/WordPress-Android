package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class CheckoutStartedTest {

    @Test
    public void mapsCheckoutResultToTokenString() {
        CheckoutStarted result = new CheckoutStarted("token_123");
        Observable<String> mappedResult = Observable.just(result).map(CheckoutStarted.TOKEN);
        expect(mappedResult.toBlocking().first()).toEqual("token_123");
    }

}