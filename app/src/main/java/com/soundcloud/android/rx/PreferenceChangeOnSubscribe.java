package com.soundcloud.android.rx;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.Subscriptions;

import android.content.SharedPreferences;

/*
 * This is an alternative to the current RxAndroid implementation.
 *
 * Our version keeps a strong reference to the subscriber to avoid garbage collection that would occur even when
 * the observable is retained.
 */
public class PreferenceChangeOnSubscribe implements Observable.OnSubscribe<String> {

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

        subscriber.add(Subscriptions.create(() -> sharedPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener)));

        sharedPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }
}
