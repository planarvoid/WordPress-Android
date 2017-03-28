package com.soundcloud.android.di;

import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.di.testimplementations.NoopAppboyWrapper;

import android.content.Context;

public class TestAnalyticsModule extends AnalyticsModule {
    public AppboyWrapper provideAppboy(Context context) {
        return new NoopAppboyWrapper();
    }
}
