package com.soundcloud.android.offline;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Func1;
import rx.subscriptions.Subscriptions;

import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Named;

public class OfflineSettingsStorage {

    private static final String LIKES_OFFLINE_SYNC_ENABLED = "likes_offline_sync";

    private final SharedPreferences sharedPreferences;

    private static final Func1<String, Boolean> FILTER_OFFLINE_LIKES_KEY = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return LIKES_OFFLINE_SYNC_ENABLED.equals(key);
        }
    };

    private Func1<String, Boolean> toValue = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String key) {
            return sharedPreferences.getBoolean(key, false);
        }
    };

    @Inject
    public OfflineSettingsStorage(@Named("OfflineSettings") SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isLikesOfflineSyncEnabled() {
        return sharedPreferences.getBoolean(LIKES_OFFLINE_SYNC_ENABLED, false);
    }

    public void setLikesOfflineSync(final boolean enabled) {
        sharedPreferences.edit().putBoolean(LIKES_OFFLINE_SYNC_ENABLED, enabled).apply();
    }

    public Observable<Boolean> getLikesOfflineSyncChanged() {
        return Observable.create(new PreferenceChangeOnSubscribe(sharedPreferences))
                .filter(FILTER_OFFLINE_LIKES_KEY)
                .map(toValue);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
    }

    /*
     * This is an alternative to the current RxAndroid implementation.
     *
     * Our version keeps a strong reference to the subscriber to avoid garbage collection that would occur even when
     * the observable is retained.
     */
    private static class PreferenceChangeOnSubscribe implements Observable.OnSubscribe<String> {

        private final SharedPreferences sharedPreferences;
        private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
                new SharedPreferences.OnSharedPreferenceChangeListener() {
                    @Override
                    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                        subscriber.onNext(key);
                    }
                };

        private Subscriber<? super String> subscriber;

        public PreferenceChangeOnSubscribe(SharedPreferences sharedPreferences) {
            this.sharedPreferences = sharedPreferences;
        }

        @Override
        public void call(final Subscriber<? super String> subscriber) {
            this.subscriber = subscriber;

            subscriber.add(Subscriptions.create(new Action0() {
                @Override
                public void call() {
                    sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
                }
            }));

            sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        }
    }

}
