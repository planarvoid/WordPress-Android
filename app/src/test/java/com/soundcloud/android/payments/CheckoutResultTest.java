package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class CheckoutResultTest {

    @Test
    public void mapsCheckoutResultToTokenString() {
        CheckoutResult result = new CheckoutResult("token_123");
        Observable<String> mappedResult = Observable.just(result).map(CheckoutResult.TOKEN);
        expect(mappedResult.toBlocking().first()).toEqual("token_123");
    }

}