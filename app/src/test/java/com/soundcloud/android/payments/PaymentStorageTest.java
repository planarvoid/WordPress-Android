package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import com.xtremelabs.robolectric.shadows.ShadowPreferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.SharedPreferences;

@RunWith(SoundCloudTestRunner.class)
public class PaymentStorageTest {

    private PaymentStorage paymentStorage;

    @Before
    public void setUp() throws Exception {
        SharedPreferences prefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application);
        paymentStorage = new PaymentStorage(prefs);
    }

    @Test
    public void savesPendingTransactionUrn() {
        paymentStorage.setCheckoutToken("blah:payment:123");
        expect(paymentStorage.getCheckoutToken()).toEqual("blah:payment:123");
    }

    @Test
    public void clearsPendingTransactionUrn() {
        paymentStorage.setCheckoutToken("blah:payment:123");
        paymentStorage.clear();
        expect(paymentStorage.getCheckoutToken()).toBeNull();
    }

}