package com.soundcloud.android.policies;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;
import android.content.SharedPreferences;

public class PolicySettingsStorageTest extends AndroidUnitTest {

    private final SharedPreferences preferences = sharedPreferences("test", Context.MODE_PRIVATE);
    private PolicySettingsStorage storage;

    @Before
    public void setUp() {
        storage = new PolicySettingsStorage(preferences);
    }

    @Test
    public void savesLastPolicyUpdateTime() {
        storage.setPolicyUpdateTime(1000L);
        assertThat(storage.getPolicyUpdateTime()).isEqualTo(1000L);
    }

    @Test
    public void savesGoBackOnlineLastShownTimestamp() {
        storage.setLastPolicyCheckTime(1000L);
        assertThat(storage.getLastPolicyCheckTime()).isEqualTo(1000L);
    }
}