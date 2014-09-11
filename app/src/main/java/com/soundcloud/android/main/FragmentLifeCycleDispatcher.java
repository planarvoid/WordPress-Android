package com.soundcloud.android.main;

import org.jetbrains.annotations.Nullable;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"PMD.CallSuperFirst", "PMD.CallSuperLast"})
public class FragmentLifeCycleDispatcher<FragmentT extends Fragment> implements FragmentLifeCycle<FragmentT> {
    private final List<FragmentLifeCycle<FragmentT>> components;

    public FragmentLifeCycleDispatcher() {
        this.components = new ArrayList<>();
    }

    public FragmentLifeCycleDispatcher add(FragmentLifeCycle<FragmentT> component) {
        this.components.add(component);
        return this;
    }

    @Override
    public void onBind(FragmentT fragment) {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onBind(fragment);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onAttach(activity);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onCreate(bundle);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onViewCreated(view, savedInstanceState);
        }
    }

    @Override
    public void onStart() {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onStart();
        }
    }

    @Override
    public void onResume() {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onResume();
        }
    }

    @Override
    public void onPause() {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onPause();
        }
    }

    @Override
    public void onStop() {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onStop();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onSaveInstanceState(bundle);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onRestoreInstanceState(bundle);
        }
    }

    @Override
    public void onDestroyView() {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onDestroyView();
        }
    }

    @Override
    public void onDestroy() {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onDestroy();
        }
    }

    @Override
    public void onDetach() {
        for (FragmentLifeCycle<FragmentT> component : components) {
            component.onDetach();
        }
    }
}
