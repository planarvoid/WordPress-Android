package com.soundcloud.android.introductoryoverlay;

import com.soundcloud.android.storage.StorageModule;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;

public class IntroductoryOverlayOperations implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final SharedPreferences sharedPreferences;
    private Set<OnIntroductoryOverlayStateChangedListener> listeners = new HashSet<>();

    @Inject
    public IntroductoryOverlayOperations(@Named(StorageModule.INTRODUCTORY_OVERLAYS) SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public void setOverlayShown(String key, boolean shown) {
        sharedPreferences.edit()
                         .putBoolean(key, shown)
                         .apply();
    }

    void setOverlayShown(String key) {
        setOverlayShown(key, true);
    }

    public boolean wasOverlayShown(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    public void registerOnStateChangedListener(OnIntroductoryOverlayStateChangedListener listener) {
        listeners.add(listener);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    public void unregisterOnStateChangedListener(OnIntroductoryOverlayStateChangedListener listener) {
        listeners.remove(listener);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        for (OnIntroductoryOverlayStateChangedListener listener : listeners) {
            listener.onIntroductoryOverlayStateChanged(key);
        }
    }

    public interface OnIntroductoryOverlayStateChangedListener {
        void onIntroductoryOverlayStateChanged(String introductoryOverlayKey);
    }
}
