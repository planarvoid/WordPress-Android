package com.soundcloud.android.discovery.welcomeuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WelcomeUserStorageTest extends AndroidUnitTest {

    @Mock private FeatureFlags featureFlags;
    private SharedPreferences sharedPreferences;
    private WelcomeUserStorage welcomeUserStorage;

    @Before
    public void setUp() throws Exception {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context());
        welcomeUserStorage = new WelcomeUserStorage(sharedPreferences, featureFlags);

        when(featureFlags.isEnabled(Flag.WELCOME_USER)).thenReturn(true);
    }

    @Test
    public void featureFlagEnabledAndNoSavedValue() throws Exception {
        assertThat(welcomeUserStorage.shouldShowWelcome()).isTrue();
    }

    @Test
    public void onWelcomeStoresData() throws Exception {
        welcomeUserStorage.onWelcomeUser();
        assertThat(sharedPreferences.contains(WelcomeUserStorage.TIMESTAMP_WELCOME_CARD)).isTrue();
    }

    @Test
    public void storeValueAndDontWelcomeAgain() throws Exception {
        when(featureFlags.isEnabled(Flag.WELCOME_USER)).thenReturn(false);
        welcomeUserStorage.onWelcomeUser();
        assertThat(welcomeUserStorage.shouldShowWelcome()).isFalse();
    }
}
