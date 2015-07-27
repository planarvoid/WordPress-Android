package com.soundcloud.android.onboarding.suggestions;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import org.jetbrains.annotations.NotNull;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.HashMap;
import java.util.Map;

public final class StateHolderFragment extends Fragment {

    private final Map<String, Object> data;

    public StateHolderFragment() {
        setRetainInstance(true);
        data = new HashMap<>();
    }

    public static StateHolderFragment obtain(@NotNull final FragmentManager fragmentManager,
                                             @NotNull final String hostFragmentTag) {
        final String stateTag = hostFragmentTag + "_state";
        StateHolderFragment fragment = (StateHolderFragment) fragmentManager.findFragmentByTag(stateTag);
        if (fragment == null) {
            fragment = new StateHolderFragment();
            fragmentManager.beginTransaction().add(fragment, stateTag).commit();
        }
        return fragment;
    }

    public <T> void put(String key, T value) {
        data.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) data.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrPut(String key, T defaultValue) {
        Class<?> clazz = defaultValue.getClass();
        Object value = data.get(key);
        if (value == null) {
            value = defaultValue;
            put(key, value);
        } else {
            checkArgument(value.getClass().isAssignableFrom(clazz),
                    "Cannot convert value found at key '" + key + "'; expected " + clazz.getCanonicalName() +
                            ", found " + value.getClass().getCanonicalName());
        }
        return (T) value;
    }

    public boolean has(String key) {
        return data.containsKey(key);
    }
}
