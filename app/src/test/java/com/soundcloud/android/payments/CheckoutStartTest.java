package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class CheckoutStartTest {

    @Test
    public void mapsCheckoutResultToTokenString() {
        CheckoutStart result = new CheckoutStart("token_123");
        Observable<String> mappedResult = Observable.just(result).map(CheckoutStart.TOKEN);
        expect(mappedResult.toBlocking().first()).toEqual("token_123");
    }

}