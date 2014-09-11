package com.soundcloud.android.main;

import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.CallSuperFirst", "PMD.CallSuperLast"})
public class ActivityLifeCycleDispatcher<ActivityT extends Activity> implements ActivityLifeCycle<ActivityT> {
    private final List<ActivityLifeCycle<ActivityT>> components;

    public ActivityLifeCycleDispatcher() {
        this.components = new ArrayList<>();
    }

    public ActivityLifeCycleDispatcher add(ActivityLifeCycle<ActivityT> component) {
        this.components.add(component);
        return this;
    }

    @Override
    public void onBind(ActivityT owner) {
        for (ActivityLifeCycle<ActivityT> component : components) {
            component.onBind(owner);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        for (ActivityLifeCycle component : components) {
            component.onCreate(bundle);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        for (ActivityLifeCycle component : components) {
            component.onNewIntent(intent);
        }
    }

    @Override
    public void onStart() {
        for (ActivityLifeCycle component : components) {
            component.onStart();
        }
    }

    @Override
    public void onResume() {
        for (ActivityLifeCycle component : components) {
            component.onResume();
        }
    }

    @Override
    public void onPause() {
        for (ActivityLifeCycle component : components) {
            component.onPause();
        }
    }

    @Override
    public void onStop() {
        for (ActivityLifeCycle component : components) {
            component.onStop();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        for (ActivityLifeCycle component : components) {
            component.onSaveInstanceState(bundle);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        for (ActivityLifeCycle component : components) {
            component.onRestoreInstanceState(bundle);
        }
    }

    @Override
    public void onDestroy() {
        for (ActivityLifeCycle component : components) {
            component.onDestroy();
        }
    }
}
