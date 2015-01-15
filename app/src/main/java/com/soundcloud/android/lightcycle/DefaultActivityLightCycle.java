package com.soundcloud.android.lightcycle;

import org.jetbrains.annotations.Nullable;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class DefaultActivityLightCycle implements ActivityLightCycle {
    @Override
    public void onCreate(FragmentActivity activity, @Nullable Bundle bundle) { /* no-op */ }

    @Override
    public void onNewIntent(FragmentActivity activity, Intent intent) { /* no-op */ }

    @Override
    public void onStart(FragmentActivity activity) { /* no-op */ }

    @Override
    public void onResume(FragmentActivity activity) { /* no-op */ }

    @Override
    public void onPause(FragmentActivity activity) { /* no-op */ }

    @Override
    public void onStop(FragmentActivity activity) { /* no-op */ }

    @Override
    public void onSaveInstanceState(FragmentActivity activity, Bundle bundle) { /* no-op */ }

    @Override
    public void onRestoreInstanceState(FragmentActivity activity, Bundle bundle) { /* no-op */ }

    @Override
    public void onDestroy(FragmentActivity activity) { /* no-op */ }
}
