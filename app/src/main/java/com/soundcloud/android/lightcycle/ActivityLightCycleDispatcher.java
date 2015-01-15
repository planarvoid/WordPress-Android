package com.soundcloud.android.lightcycle;

import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import java.util.HashSet;
import java.util.Set;

public final class ActivityLightCycleDispatcher implements ActivityLightCycle {
    private final Set<ActivityLightCycle> activityLightCycles;

    public ActivityLightCycleDispatcher() {
        this.activityLightCycles = new HashSet<>();
    }

    public ActivityLightCycleDispatcher add(ActivityLightCycle component) {
        this.activityLightCycles.add(component);
        return this;
    }

    @Override
    public void onCreate(FragmentActivity activity, @Nullable Bundle bundle) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onCreate(activity, bundle);
        }
    }

    @Override
    public void onNewIntent(FragmentActivity activity, Intent intent) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onNewIntent(activity, intent);
        }
    }

    @Override
    public void onStart(FragmentActivity activity) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onStart(activity);
        }
    }

    @Override
    public void onResume(FragmentActivity activity) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onResume(activity);
        }
    }

    @Override
    public void onPause(FragmentActivity activity) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onPause(activity);
        }
    }

    @Override
    public void onStop(FragmentActivity activity) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onStop(activity);
        }
    }

    @Override
    public void onSaveInstanceState(FragmentActivity activity, Bundle bundle) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onSaveInstanceState(activity, bundle);
        }
    }

    @Override
    public void onRestoreInstanceState(FragmentActivity activity, Bundle bundle) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onRestoreInstanceState(activity, bundle);
        }
    }

    @Override
    public void onDestroy(FragmentActivity activity) {
        for (ActivityLightCycle component : activityLightCycles) {
            component.onDestroy(activity);
        }
    }
}
