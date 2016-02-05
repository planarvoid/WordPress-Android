package com.soundcloud.android.configuration;

import static com.soundcloud.android.configuration.ConfigurationManager.TAG;

import com.soundcloud.android.rx.PreferenceChangeOnSubscribe;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.Log;
import rx.Observable;
import rx.functions.Func1;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.List;

public class PlanStorage {

    private static final String KEY_PLAN = "plan";
    private static final String KEY_UPSELLS = "upsells";
    private static final Func1<String, Boolean> UPSELLS_PREFERENCE = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return KEY_UPSELLS.equals(key);
        }
    };

    private final SharedPreferences sharedPreferences;
    private final Func1<String, List<Plan>> toUpsells = new Func1<String, List<Plan>>() {
        @Override
        public List<Plan> call(String key) {
            return getUpsells();
        }
    };

    @Inject
    public PlanStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void updatePlan(Plan plan) {
        Log.d(TAG, "updating plan: " + plan);
        sharedPreferences.edit().putString(KEY_PLAN, plan.planId).apply();
    }

    public void updateUpsells(List<Plan> plans) {
        Log.d(TAG, "updating upsells: " + plans);
        sharedPreferences.edit().putStringSet(KEY_UPSELLS, Plan.toIds(plans)).apply();
    }

    public Plan getPlan() {
        return Plan.fromId(sharedPreferences.getString(KEY_PLAN, Plan.FREE_TIER.planId));
    }

    public List<Plan> getUpsells() {
        return Plan.fromIds(sharedPreferences.getStringSet(KEY_UPSELLS, Collections.<String>emptySet()));
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
