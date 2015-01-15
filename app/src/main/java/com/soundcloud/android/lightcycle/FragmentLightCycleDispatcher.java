package com.soundcloud.android.lightcycle;

import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.HashSet;
import java.util.Set;

public final class FragmentLightCycleDispatcher implements FragmentLightCycle {
    private final Set<FragmentLightCycle> fragmentLightCycles;

    public FragmentLightCycleDispatcher() {
        this.fragmentLightCycles = new HashSet<>();
    }

    public FragmentLightCycleDispatcher add(FragmentLightCycle lightCycle) {
        fragmentLightCycles.add(lightCycle);
        return this;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onCreate(fragment, bundle);
        }
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onViewCreated(fragment, view, savedInstanceState);
        }
    }

    @Override
    public void onStart(Fragment fragment) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onStart(fragment);
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onResume(fragment);
        }
    }

    @Override
    public void onPause(Fragment fragment) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onPause(fragment);
        }
    }

    @Override
    public void onStop(Fragment fragment) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onStop(fragment);
        }
    }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle bundle) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onSaveInstanceState(fragment, bundle);
        }
    }

    @Override
    public void onRestoreInstanceState(Fragment fragment, Bundle bundle) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onRestoreInstanceState(fragment, bundle);
        }
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onDestroyView(fragment);
        }
    }

    @Override
    public void onDestroy(Fragment fragment) {
        for (FragmentLightCycle component : fragmentLightCycles) {
            component.onDestroy(fragment);
        }
    }
}
