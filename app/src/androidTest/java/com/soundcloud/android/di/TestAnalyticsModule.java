package com.soundcloud.android.di;

import com.soundcloud.android.analytics.AnalyticsModule;
import com.soundcloud.android.analytics.appboy.AppboyInAppMessageListener;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.di.testimplementations.NoopAppboyWrapper;

import android.content.Context;

public class TestAnalyticsModule extends AnalyticsModule {
    @Override
    public AppboyWrapper provideAppboy(Context context, AppboyInAppMessageListener listener) {
        return new NoopAppboyWrapper();
    }
}
