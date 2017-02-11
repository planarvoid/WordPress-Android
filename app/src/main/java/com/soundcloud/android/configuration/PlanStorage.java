package com.soundcloud.android.configuration;

import com.soundcloud.android.configuration.UserPlan.Upsell;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

public class PlanStorage {

    private static final String KEY_PLAN = "plan";
    private static final String KEY_MANAGEABLE = "manageable";
    private static final String KEY_VENDOR = "vendor";
    private static final String KEY_UPSELLS = "upsells";
    private static final String KEY_HIGH_TIER_TRIAL = "high_tier_trial";

    private static final int NO_TRIAL = 0;

    private static final Func1<String, Boolean> UPSELLS_PREFERENCE = key -> KEY_UPSELLS.equals(key);

    private final SharedPreferences sharedPreferences;
    private final Func1<String, List<Plan>> toUpsells = key -> getUpsells();

    @Inject
    public PlanStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    void updatePlan(Plan plan) {
        sharedPreferences.edit().putString(KEY_PLAN, plan.planId).apply();
    }

    void updateManageable(boolean manageable) {
        sharedPreferences.edit().putBoolean(KEY_MANAGEABLE, manageable).apply();
    }

    void updateVendor(Optional<String> vendor) {
        if(vendor.isPresent()) {
            sharedPreferences.edit().putString(KEY_VENDOR, vendor.get()).apply();
        } else {
            sharedPreferences.edit().remove(KEY_VENDOR).apply();
        }
    }

    public void updateUpsells(List<Upsell> upsells) {
        List<Plan> plans = Plan.fromUpsells(upsells);
        sharedPreferences.edit().putStringSet(KEY_UPSELLS, Plan.toIds(plans)).apply();
        updateTrialDays(upsells);
    }

    private void updateTrialDays(List<Upsell> upsells) {
        int highTierTrialDays = NO_TRIAL;
        for (Upsell upsell : upsells) {
            if (Plan.fromId(upsell.id) == Plan.HIGH_TIER) {
                highTierTrialDays = upsell.trialDays;
            }
        }
        sharedPreferences.edit().putInt(KEY_HIGH_TIER_TRIAL, highTierTrialDays).apply();
    }

    public Plan getPlan() {
        return Plan.fromId(sharedPreferences.getString(KEY_PLAN, Plan.UNDEFINED.planId));
    }

    boolean isManageable() {
        return sharedPreferences.getBoolean(KEY_MANAGEABLE, false);
    }

    String getVendor() {
        return sharedPreferences.getString(KEY_VENDOR, Strings.EMPTY);
    }

    public List<Plan> getUpsells() {
        return Plan.fromIds(sharedPreferences.getStringSet(KEY_UPSELLS, Collections.<String>emptySet()));
    }

    int getHighTierTrialDays() {
        return sharedPreferences.getInt(KEY_HIGH_TIER_TRIAL, NO_TRIAL);
    }

    public Observable<List<Plan>> getUpsellUpdates() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                         .filter(UPSELLS_PREFERENCE)
                         .map(toUpsells);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
