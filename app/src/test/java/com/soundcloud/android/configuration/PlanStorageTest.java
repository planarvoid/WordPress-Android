package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import org.junit.Before;
import org.junit.Test;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Arrays;

public class PlanStorageTest extends PlatformUnitTest {

    private PlanStorage storage;

    @Before
    public void setUp() throws Exception {
        SharedPreferences prefs = new ObfuscatedPreferences(sharedPreferences("test", Context.MODE_PRIVATE), new Obfuscator());
        storage = new PlanStorage(prefs);
    }

    @Test
    public void updateStoresValue() {
        storage.updatePlan(Plan.MID_TIER);

        assertThat(storage.getPlan()).isEqualTo(Plan.MID_TIER);
    }

    @Test
    public void returnsDefaultIfNotSet() {
        assertThat(storage.getPlan()).isEqualTo(Plan.NONE);
    }

    @Test
    public void updateStoresValueList() {
        storage.updateUpsells(Arrays.asList(Plan.MID_TIER, Plan.HIGH_TIER));

        assertThat(storage.getUpsells()).containsOnly(Plan.MID_TIER, Plan.HIGH_TIER);
    }

    @Test
    public void updateReplacesEntireValueList() {
        storage.updateUpsells(Arrays.asList(Plan.MID_TIER, Plan.HIGH_TIER));

        storage.updateUpsells(Arrays.asList(Plan.MID_TIER));

        assertThat(storage.getUpsells()).containsExactly(Plan.MID_TIER);
    }

    @Test
    public void returnsEmptyListIfNotSet() {
        assertThat(storage.getUpsells()).isEmpty();
    }

    @Test
    public void clearRemovesStoredValues() {
        storage.updatePlan(Plan.MID_TIER);

        storage.clear();

        assertThat(storage.getPlan()).isEqualTo(Plan.NONE);
    }

}