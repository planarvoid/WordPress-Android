package com.soundcloud.android.main;

import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings({"PMD.CallSuperFirst", "PMD.CallSuperLast", "PMD.MissingStaticMethodInNonInstantiatableClass"})
public class FragmentLifeCycleDispatcher<FragmentT extends Fragment> implements FragmentLifeCycle<FragmentT> {
    private final Collection<FragmentLifeCycle<FragmentT>> fragmentLifeCycles;

    private FragmentLifeCycleDispatcher(Collection<FragmentLifeCycle<FragmentT>> fragmentLifeCycles) {
        this.fragmentLifeCycles = fragmentLifeCycles;
    }

    @Override
    public void onBind(FragmentT fragment) {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onBind(fragment);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onCreate(bundle);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onViewCreated(view, savedInstanceState);
        }
    }

    @Override
    public void onStart() {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onStart();
        }
    }

    @Override
    public void onResume() {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onResume();
        }
    }

    @Override
    public void onPause() {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onPause();
        }
    }

    @Override
    public void onStop() {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onStop();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onSaveInstanceState(bundle);
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle bundle) {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onRestoreInstanceState(bundle);
        }
    }

    @Override
    public void onDestroyView() {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onDestroyView();
        }
    }

    @Override
    public void onDestroy() {
        for (FragmentLifeCycle<FragmentT> component : fragmentLifeCycles) {
            component.onDestroy();
        }
    }

    @SuppressWarnings("PMD.AccessorClassGeneration")
    public static class Builder<FragmentT extends Fragment> {
        private final List<FragmentLifeCycle<FragmentT>> components;

        public Builder() {
            components = new ArrayList<>();
        }


        public Builder add(FragmentLifeCycle<FragmentT> component) {
            this.components.add(component);
            return this;
        }

        public FragmentLifeCycleDispatcher<FragmentT> build() {
            return new FragmentLifeCycleDispatcher(Collections.unmodifiableCollection(components));
        }
    }
}
