package com.soundcloud.android.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.configuration.UserPlan.Upsell;
import com.soundcloud.android.crypto.Obfuscator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.ObfuscatedPreferences;
import com.soundcloud.java.optional.Optional;
import org.junit.Before;
import org.junit.Test;

import android.content.SharedPreferences;

import java.util.Arrays;
import java.util.Collections;

public class PlanStorageTest extends AndroidUnitTest {

    private PlanStorage storage;

    private static final Upsell MID_TIER = new Upsell(Plan.MID_TIER.planId, 0);
    private static final Upsell HIGH_TIER = new Upsell(Plan.HIGH_TIER.planId, 30);

    @Before
    public void setUp() throws Exception {
        SharedPreferences prefs = new ObfuscatedPreferences(sharedPreferences(), new Obfuscator());
        storage = new PlanStorage(prefs);
    }

    @Test
    public void updateStoresValue() {
        storage.updatePlan(Plan.HIGH_TIER);

        assertThat(storage.getPlan()).isEqualTo(Plan.HIGH_TIER);
    }

    @Test
    public void returnsUndefinedIfNotSet() {
        assertThat(storage.getPlan()).isEqualTo(Plan.UNDEFINED);
    }

    @Test
    public void shouldUpdateManageableValue() {
        storage.updateManageable(true);

        assertThat(storage.isManageable()).isTrue();
    }

    @Test
    public void shouldUpdateVendorValueIfPresent() {
        Optional<String> vendor = Optional.of("apple");
        storage.updateVendor(vendor);
        assertThat(storage.getVendor()).isEqualTo(vendor.get());
    }

    @Test
    public void shouldNotUpdateVendorValueIfAbsent() {
        Optional<String> vendor = Optional.absent();
        storage.updateVendor(vendor);
        assertThat(storage.getVendor()).isEmpty();
    }

    @Test
    public void shouldReturnEmptyStringIfVendorValueNotSet() {
        assertThat(storage.getVendor()).isEmpty();
    }

    @Test
    public void updateStoresValueList() {
        storage.updateUpsells(Arrays.asList(MID_TIER, HIGH_TIER));

        assertThat(storage.getUpsells()).containsOnly(Plan.MID_TIER, Plan.HIGH_TIER);
    }

    @Test
    public void updateReplacesEntireValueList() {
        storage.updateUpsells(Arrays.asList(MID_TIER, HIGH_TIER));

        storage.updateUpsells(Collections.singletonList(MID_TIER));

        assertThat(storage.getUpsells()).containsExactly(Plan.MID_TIER);
    }

    @Test
    public void returnsEmptyListIfNotSet() {
        assertThat(storage.getUpsells()).isEmpty();
    }

    @Test
    public void updateSetsHighTierTrialDays() {
        storage.updateUpsells(Collections.singletonList(HIGH_TIER));

        assertThat(storage.getHighTierTrialDays()).isEqualTo(30);
    }

    @Test
    public void updateClearsHighTierTrialDaysIfUpsellIsNotPresent() {
        storage.updateUpsells(Collections.singletonList(HIGH_TIER));
        storage.updateUpsells(Collections.singletonList(MID_TIER));

        assertThat(storage.getHighTierTrialDays()).isEqualTo(0);
    }

    @Test
    public void clearRemovesStoredValues() {
        storage.updatePlan(Plan.HIGH_TIER);

        storage.clear();

        assertThat(storage.getPlan()).isEqualTo(Plan.UNDEFINED);
    }

}
