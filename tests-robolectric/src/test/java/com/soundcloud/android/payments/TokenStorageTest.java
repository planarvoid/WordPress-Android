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
public class TokenStorageTest {

    private TokenStorage tokenStorage;

    @Before
    public void setUp() throws Exception {
        SharedPreferences prefs = ShadowPreferenceManager.getDefaultSharedPreferences(Robolectric.application);
        tokenStorage = new TokenStorage(prefs);
    }

    @Test
    public void savesPendingTransactionUrn() {
        tokenStorage.setCheckoutToken("blah:payment:123");
        expect(tokenStorage.getCheckoutToken()).toEqual("blah:payment:123");
    }

    @Test
    public void clearsPendingTransactionUrn() {
        tokenStorage.setCheckoutToken("blah:payment:123");
        tokenStorage.clear();
        expect(tokenStorage.getCheckoutToken()).toBeNull();
    }

}