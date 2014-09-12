package com.soundcloud.android.main;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public class DefaultFragment extends Fragment {

    private final FragmentLifeCycleDispatcher.Builder<Fragment> lifeCycleDispatcherBuilder;
    private FragmentLifeCycleDispatcher<Fragment> lifeCycleDispatcher;

    public DefaultFragment() {
        lifeCycleDispatcherBuilder = new FragmentLifeCycleDispatcher.Builder();
    }

    public void addLifeCycleComponent(FragmentLifeCycle<Fragment> lifeCycleComponent) {
        lifeCycleDispatcherBuilder.add(lifeCycleComponent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lifeCycleDispatcher = lifeCycleDispatcherBuilder.build();
        lifeCycleDispatcher.onBind(this);
        lifeCycleDispatcher.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        lifeCycleDispatcher.onStart();
    }

    @Override
    public void onStop() {
        lifeCycleDispatcher.onStop();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        lifeCycleDispatcher.onResume();
    }

    @Override
    public void onPause() {
        lifeCycleDispatcher.onPause();
        super.onPause();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        lifeCycleDispatcher.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        lifeCycleDispatcher.onDestroyView();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        lifeCycleDispatcher.onDestroy();
        super.onDestroy();
    }
}
