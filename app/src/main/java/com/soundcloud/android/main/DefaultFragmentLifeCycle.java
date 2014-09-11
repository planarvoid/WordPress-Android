package com.soundcloud.android.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

@SuppressWarnings({"PMD.EmptyMethodInAbstractClassShouldBeAbstract", "PMD.CallSuperFirst", "PMD.CallSuperLast"})
public abstract class DefaultFragmentLifeCycle<FragmentT extends Fragment> implements FragmentLifeCycle<FragmentT> {

    @Override
    public void onBind(FragmentT fragment) {
        /* no-op */
    }

    @Override
    public void onAttach(Activity activity) {
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
    public void onDetach() {
        /* no-op */
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        /* no-op */
    }

    @Override
    public void onDestroyView() {
        /* no-op */
    }
}
