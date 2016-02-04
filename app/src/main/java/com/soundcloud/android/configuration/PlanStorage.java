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

    private static final String PLAN = "plan";
    private static final String UPSELLS = "upsells";

    private final SharedPreferences sharedPreferences;

    @Inject
    public PlanStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void updatePlan(Plan plan) {
        sharedPreferences.edit().putString(PLAN, plan.planId).apply();
    }

    public void updateUpsells(List<Plan> plans) {
        Log.d(TAG, "updating upsells: " + plans);
        sharedPreferences.edit().putStringSet(UPSELLS, Plan.toIds(plans)).apply();
    }

    public Plan getPlan() {
        return Plan.fromId(sharedPreferences.getString(PLAN, Plan.FREE_TIER.planId));
    }

    public List<Plan> getUpsells() {
        return Plan.fromIds(sharedPreferences.getStringSet(UPSELLS, Collections.<String>emptySet()));
    }

    public Observable<List<Plan>> getUpsellUpdates() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s.equals(UPSELLS);
                    }
                })
                .map(new Func1<String, List<Plan>>() {
                    @Override
                    public List<Plan> call(String s) {
                        return getUpsells();
                    }
                });
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
