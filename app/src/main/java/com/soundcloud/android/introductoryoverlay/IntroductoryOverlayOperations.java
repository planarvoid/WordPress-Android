package com.soundcloud.android.introductoryoverlay;

import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.utils.CurrentDateProvider;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class IntroductoryOverlayOperations implements SharedPreferences.OnSharedPreferenceChangeListener {
    
    private static final String OVERLAY_SHOWN_TIME = "overlay_shown_time";
    private static final long DELAY_DURATION = TimeUnit.SECONDS.toMillis(1L);

    private final SharedPreferences sharedPreferences;
    private final CurrentDateProvider dateProvider;
    private final Set<OnIntroductoryOverlayStateChangedListener> listeners = new HashSet<>();

    @Inject
    public IntroductoryOverlayOperations(@Named(StorageModule.INTRODUCTORY_OVERLAYS) SharedPreferences sharedPreferences,
                                         CurrentDateProvider dateProvider) {
        this.sharedPreferences = sharedPreferences;
        this.dateProvider = dateProvider;
    }

    public void setOverlayShown(String key, boolean shown) {
        sharedPreferences.edit()
                         .putBoolean(key, shown)
                         .putLong(OVERLAY_SHOWN_TIME, dateProvider.getCurrentTime())
                         .apply();
    }

    void setOverlayShown(String key) {
        setOverlayShown(key, true);
    }

    public boolean wasOverlayShown(String key) {
        return sharedPreferences.getBoolean(key, false);
    }

    boolean hasDelayDurationPassed() {
        final long overlayShownTime = sharedPreferences.getLong(OVERLAY_SHOWN_TIME, 1L);
        return dateProvider.getCurrentTime() > (overlayShownTime + DELAY_DURATION);
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
            if (IntroductoryOverlayKey.ALL_KEYS.contains(key)) {
                listener.onIntroductoryOverlayStateChanged(key);
            }
        }
    }

    public interface OnIntroductoryOverlayStateChangedListener {
        void onIntroductoryOverlayStateChanged(String introductoryOverlayKey);
    }
}
