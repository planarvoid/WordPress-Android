package com.soundcloud.android.stations;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.propeller.PropellerDatabase;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;

public class StationsStoragePreferencesTest extends AndroidUnitTest {

    private StationsStorage storage;

    @Before
    public void setUp() throws Exception {
        storage = new StationsStorage(
                sharedPreferences("whatever", Context.MODE_PRIVATE),
                mock(PropellerDatabase.class),
                new TestDateProvider());
    }

    @Test
    public void isOnboardingDisabledReturnsFalseWhenStorageIsEmpty() {
        assertThat(storage.isOnboardingDisabled()).isFalse();
    }

    @Test
    public void isOnboardingDisabledReturnsTrueWhenHasBeenDisabled() {
        storage.disableOnboarding();
        assertThat(storage.isOnboardingDisabled()).isTrue();
    }

    @Test
    public void clearReset() {
        storage.disableOnboarding();

        storage.clear();

        assertThat(storage.isOnboardingDisabled()).isFalse();
    }
}