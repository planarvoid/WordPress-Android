package com.soundcloud.android.discovery.welcomeuser;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WelcomeUserStorageTest extends AndroidUnitTest {

    private SharedPreferences sharedPreferences;
    private WelcomeUserStorage welcomeUserStorage;

    @Before
    public void setUp() throws Exception {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context());
        welcomeUserStorage = new WelcomeUserStorage(sharedPreferences);
    }

    @Test
    public void noStoredValueShowWelcome() throws Exception {
        assertThat(welcomeUserStorage.shouldShowWelcome()).isTrue();
    }

    @Test
    public void onWelcomeStoresData() throws Exception {
        welcomeUserStorage.onWelcomeUser();
        assertThat(sharedPreferences.contains(WelcomeUserStorage.TIMESTAMP_WELCOME_CARD)).isTrue();
    }

    @Test
    public void storeValueAndDontWelcomeAgain() throws Exception {
        welcomeUserStorage.onWelcomeUser();
        assertThat(welcomeUserStorage.shouldShowWelcome()).isFalse();
    }
}
