package com.soundcloud.android.lightcycle;

import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public interface FragmentLightCycle {
    void onCreate(Fragment fragment, @Nullable Bundle bundle);
    void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState);
    void onStart(Fragment fragment);
    void onResume(Fragment fragment);
    void onPause(Fragment fragment);
    void onStop(Fragment fragment);
    void onSaveInstanceState(Fragment fragment, Bundle bundle);
    void onRestoreInstanceState(Fragment fragment, Bundle bundle);
    void onDestroyView(Fragment fragment);
    void onDestroy(Fragment fragment);
}
