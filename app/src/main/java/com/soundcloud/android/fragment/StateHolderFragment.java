package com.soundcloud.android.fragment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import java.util.HashMap;

public final class StateHolderFragment extends Fragment {

    public static StateHolderFragment obtain(Fragment hostFragment) {
        final FragmentManager fragmentManager = hostFragment.getFragmentManager();
        final String tag = hostFragment.getClass().getCanonicalName() + "State";
        StateHolderFragment fragment = (StateHolderFragment) fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = new StateHolderFragment();
            fragmentManager.beginTransaction().add(fragment, tag).commit();
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
