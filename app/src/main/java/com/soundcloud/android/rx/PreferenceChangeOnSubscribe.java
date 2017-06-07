package com.soundcloud.android.rx;

import io.reactivex.Emitter;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

import android.content.SharedPreferences;

/*
 * This is an alternative to the current RxAndroid implementation.
 *
 * Our version keeps a strong reference to the subscriber to avoid garbage collection that would occur even when
 * the observable is retained.
 */
public class PreferenceChangeOnSubscribe implements ObservableOnSubscribe<String> {

    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    emitter.onNext(key);
                }
            };

    private Emitter<? super String> emitter;

    public PreferenceChangeOnSubscribe(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public void subscribe(ObservableEmitter<String> emitter) throws Exception {
        this.emitter = emitter;

        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
        emitter.setCancellable(() -> sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener));
    }
}
