package com.soundcloud.android.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

public abstract class DefaultFragment extends Fragment implements LifeCycleOwner<FragmentLifeCycle<Fragment>> {

    private final FragmentLifeCycleDispatcher<Fragment> lifeCycleDispatcher = new FragmentLifeCycleDispatcher<>();

    @Override
    public void addLifeCycleComponent(FragmentLifeCycle<Fragment> lifeCycleComponent) {
        lifeCycleDispatcher.add(lifeCycleComponent);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        addLifeCycleComponents();
        lifeCycleDispatcher.onAttach(activity);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    @Override
    public void onDetach() {
        lifeCycleDispatcher.onDetach();
        super.onDetach();
    }
}
