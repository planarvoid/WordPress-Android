package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;

public class TokenStorageTest extends AndroidUnitTest {

    private TokenStorage tokenStorage;

    @Before
    public void setUp() throws Exception {
        tokenStorage = new TokenStorage(sharedPreferences("test", Context.MODE_PRIVATE));
    }

    @Test
    public void savesPendingTransactionUrn() {
        tokenStorage.setCheckoutToken("blah:payment:123");
        assertThat(tokenStorage.getCheckoutToken()).isEqualTo("blah:payment:123");
    }

    @Test
    public void clearsPendingTransactionUrn() {
        tokenStorage.setCheckoutToken("blah:payment:123");
        tokenStorage.clear();
        assertThat(tokenStorage.getCheckoutToken()).isNull();
    }

}