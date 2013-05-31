package com.soundcloud.android.fragment;

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

    public <T> T getOrDefault(String key, Class<T> type, T defaultValue) {
        T value = type.cast(mData.get(key));
        if (value == null) {
            value = defaultValue;
            put(key, value);
        }
        return value;
    }

    public boolean has(String key) {
        return mData.containsKey(key);
    }
}
