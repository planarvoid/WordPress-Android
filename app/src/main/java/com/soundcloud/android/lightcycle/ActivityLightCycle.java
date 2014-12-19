package com.soundcloud.android.lightcycle;

import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public interface ActivityLightCycle {
    void onCreate(FragmentActivity activity, @Nullable Bundle bundle);
    void onNewIntent(FragmentActivity activity, Intent intent);
    void onStart(FragmentActivity activity);
    void onResume(FragmentActivity activity);
    void onPause(FragmentActivity activity);
    void onStop(FragmentActivity activity);
    void onSaveInstanceState(FragmentActivity activity, Bundle bundle);
    void onRestoreInstanceState(FragmentActivity activity, Bundle bundle);
    void onDestroy(FragmentActivity activity);
}
