package com.soundcloud.android.main;

import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"PMD.CallSuperFirst", "PMD.CallSuperLast"})
public class ActivityLifeCycleDispatcher<ActivityT extends Activity> implements ActivityLifeCycle<ActivityT> {
    private final Collection<ActivityLifeCycle<ActivityT>> activityLifeCycles;

    private ActivityLifeCycleDispatcher(Collection<ActivityLifeCycle<ActivityT>> activityLifeCycles) {
        this.activityLifeCycles = activityLifeCycles;
    }

    @Override
    public void onBind(ActivityT owner) {
        for (ActivityLifeCycle<ActivityT> component : activityLifeCycles) {
            component.onBind(owner);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onCreate(bundle);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onNewIntent(intent);
        }
    }

    @Override
    public void onStart() {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onStart();
        }
    }

    @Override
    public void onResume() {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onResume();
        }
    }

    @Override
    public void onPause() {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onPause();
        }
    }

    @Override
    public void onStop() {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onStop();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onSaveInstanceState(bundle);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onRestoreInstanceState(bundle);
        }
    }

    @Override
    public void onDestroy() {
        for (ActivityLifeCycle component : activityLifeCycles) {
            component.onDestroy();
        }
    }

    public static class Builder<ActivityT extends Activity> {
        private final List<ActivityLifeCycle<ActivityT>> components;

        public Builder() {
            components = new ArrayList<>();
        }


        public Builder add(ActivityLifeCycle<ActivityT> component) {
            this.components.add(component);
            return this;
        }

        public ActivityLifeCycleDispatcher<ActivityT> build() {
            return new ActivityLifeCycleDispatcher(Collections.unmodifiableCollection(components));
        }
    }
}
