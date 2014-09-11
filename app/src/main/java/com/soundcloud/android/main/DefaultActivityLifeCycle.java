package com.soundcloud.android.main;

import android.content.Intent;
import android.os.Bundle;

@SuppressWarnings({"PMD.EmptyMethodInAbstractClassShouldBeAbstract", "PMD.CallSuperFirst", "PMD.CallSuperLast"})
public abstract class DefaultActivityLifeCycle<ActivityT extends ScActivity> implements ActivityLifeCycle<ActivityT> {

    @Override
    public void onBind(ActivityT owner) {
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

    @Override
    public void onNewIntent(Intent intent) {
        /* no-op */
    }
}
