package com.soundcloud.android.main;

import com.soundcloud.android.actionbar.ActionBarController;

import android.app.Activity;
import android.os.Bundle;

@SuppressWarnings({"PMD.EmptyMethodInAbstractClassShouldBeAbstract", "PMD.CallSuperFirst"})
public abstract class DefaultLifeCycleComponent implements LifeCycleComponent {

    @Override
    public void attach(Activity activity, ActionBarController actionBarController) {
        /* no-op */
    }

    @Override
    public void onCreate(Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onStart() {
        /* no-op */
    }

    @Override
    public void onResume() {
        /* no-op */
    }

    @Override
    public void onPause() {
        /* no-op */
    }

    @Override
    public void onStop() {
        /* no-op */
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        /* no-op */
    }

    @Override
    public void onDestroy() {
        /* no-op */
    }
}
