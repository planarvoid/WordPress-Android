package com.soundcloud.android.lightcycle;

import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public abstract class DefaultFragmentLightCycle implements FragmentLightCycle {
    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) { /* no-op */ }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) { /* no-op */ }

    @Override
    public void onStart(Fragment fragment) { /* no-op */ }

    @Override
    public void onResume(Fragment fragment) { /* no-op */ }

    @Override
    public void onPause(Fragment fragment) { /* no-op */ }

    @Override
    public void onStop(Fragment fragment) { /* no-op */ }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle bundle) { /* no-op */ }

    @Override
    public void onRestoreInstanceState(Fragment fragment, Bundle bundle) { /* no-op */ }

    @Override
    public void onDestroyView(Fragment fragment) { /* no-op */ }

    @Override
    public void onDestroy(Fragment fragment) { /* no-op */ }
}
