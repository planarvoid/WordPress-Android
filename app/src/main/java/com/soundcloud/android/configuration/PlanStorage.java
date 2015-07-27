package com.soundcloud.android.configuration;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.java.collections.Lists;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class PlanStorage {

    private static final String PLAN = "plan";
    private static final String UPSELLS = "upsells";

    private final SharedPreferences sharedPreferences;

    @Inject
    public PlanStorage(@Named(StorageModule.FEATURES) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void updatePlan(String value) {
        sharedPreferences.edit().putString(PLAN, value).apply();
    }

    public void updateUpsells(List<String> values) {
        sharedPreferences.edit().putStringSet(UPSELLS, new HashSet<>(values)).apply();
    }

    public String getPlan() {
        return sharedPreferences.getString(PLAN, Plan.NONE);
    }

    public List<String> getUpsells() {
        return Lists.newArrayList(sharedPreferences.getStringSet(UPSELLS, Collections.<String>emptySet()));
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

}
