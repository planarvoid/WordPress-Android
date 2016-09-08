package com.soundcloud.android.stations;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.PropellerDatabase;
import org.junit.Before;
import org.junit.Test;

public class StationsStoragePreferencesTest extends AndroidUnitTest {

    private StationsStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new StationsStorage(sharedPreferences(), mock(PropellerDatabase.class), new TestDateProvider());
    }

    @Test
    public void isOnboardingStreamItemDisabledReturnsFalseWhenStorageIsEmpty() {
        assertThat(storage.isOnboardingStreamItemDisabled()).isFalse();
    }

    @Test
    public void isOnboardingStreamItemDisabledReturnsTrueWhenHasBeenDisabled() {
        storage.disableOnboardingStreamItem();
        assertThat(storage.isOnboardingStreamItemDisabled()).isTrue();
    }

    @Test
    public void isOnboardingForLikedStationsDisabledReturnsFalseWhenStorageIsEmpty() {
        assertThat(storage.isOnboardingForLikedStationsDisabled()).isFalse();
    }

    @Test
    public void isOnboardingForLikedStationsDisabledReturnsTrueAfterItGotDisabled() {
        storage.disableLikedStationsOnboarding();
        assertThat(storage.isOnboardingForLikedStationsDisabled()).isTrue();
    }

    @Test
    public void clearReset() {
        storage.disableOnboardingStreamItem();

        storage.clear();

        assertThat(storage.isOnboardingStreamItemDisabled()).isFalse();
    }
}
