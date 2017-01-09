package com.soundcloud.android.configuration;

import static com.soundcloud.android.configuration.ConfigurationManager.TAG;

import com.soundcloud.android.configuration.UserPlan.Upsell;
import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlanStorage {

    private static final String KEY_PLAN = "plan";
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
        Log.d(TAG, "updating plan: " + plan);
        sharedPreferences.edit().putString(KEY_PLAN, plan.planId).apply();
    }

    public void updateUpsells(List<Upsell> upsells) {
        List<Plan> plans = Plan.fromUpsells(upsells);
        Log.d(TAG, "updating upsells: " + Arrays.toString(plans.toArray()));
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

    public List<Plan> getUpsells() {
        return Plan.fromIds(sharedPreferences.getStringSet(KEY_UPSELLS, Collections.<String>emptySet()));
    }

    public int getHighTierTrialDays() {
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
