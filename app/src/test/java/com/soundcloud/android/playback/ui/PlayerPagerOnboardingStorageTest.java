package com.soundcloud.android.playback.ui;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;

public class PlayerPagerOnboardingStorageTest extends AndroidUnitTest {
    private final PlayerPagerOnboardingStorage storage = new PlayerPagerOnboardingStorage(sharedPreferences());

    @Test
    public void initNumberOfOnboardingRunTo0() {
        assertThat(storage.numberOfOnboardingRun()).isEqualTo(0);
    }

    @Test
    public void testIncreaseNumberOfOnboardingRun() {
        storage.increaseNumberOfOnboardingRun();
        assertThat(storage.numberOfOnboardingRun()).isEqualTo(1);
    }

}
