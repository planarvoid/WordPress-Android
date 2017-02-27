package com.soundcloud.android.di;

import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.di.testimplementations.NoopAppboyWrapper;
import dagger.Provides;

import android.content.Context;

import javax.inject.Singleton;


public class TestAnalyticsModule extends AnalyticsModule {

    @Provides
    @Singleton
    public AppboyWrapper provideAppboy(Context context) {
        return new NoopAppboyWrapper();
    }
}
