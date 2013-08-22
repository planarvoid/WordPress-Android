package com.soundcloud.android.fragment;

import static com.google.common.base.Preconditions.checkArgument;

import org.jetbrains.annotations.NotNull;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.HashMap;

public final class StateHolderFragment extends Fragment {

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

    private final HashMap<String, Object> mData;

    public StateHolderFragment() {
        setRetainInstance(true);
        mData = new HashMap<String, Object>();
    }

    public <T> void put(String key, T value) {
        mData.put(key, value);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) mData.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrPut(String key, T defaultValue) {
        Class<?> clazz = defaultValue.getClass();
        Object value = mData.get(key);
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
        return mData.containsKey(key);
    }
}
