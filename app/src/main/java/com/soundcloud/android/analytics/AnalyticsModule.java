package com.soundcloud.android.analytics;

import com.appboy.Appboy;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import com.soundcloud.android.analytics.appboy.RealAppboyWrapper;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;
import org.jetbrains.annotations.Nullable;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Module
public class AnalyticsModule {

    static final String TRACKING_DB = "TrackingDB";
    static final String BASE_PROVIDERS = "BaseProviders";

    @Provides
    @Singleton
    protected AppboyWrapper provideAppboy(Context context) {
        return new RealAppboyWrapper(Appboy.getInstance(context));
    }

    @Provides
    @Nullable
    static ComScoreAnalyticsProvider provideComScoreProvider(Context context) {
        // cf. https://github.com/soundcloud/android-listeners/issues/1811
        try {
            return new ComScoreAnalyticsProvider(context);
        } catch (Exception e) {
            ErrorUtils.handleSilentException("Error during Comscore library init", e);
            return null;
        }
    }

    @Provides
    @Singleton
    static FirebaseAnalytics provideFirebaseAnalytics(Context context) {
        final FirebaseAnalytics firebaseAnalytics = FirebaseAnalytics.getInstance(context);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        return firebaseAnalytics;
    }

    @Provides
    @Named(BASE_PROVIDERS)
    static List<AnalyticsProvider> provideBaseProviders(EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider,
                                                 PromotedAnalyticsProvider promotedAnalyticsProvider) {
        List<AnalyticsProvider> providers = new ArrayList<>(3);
        providers.add(eventLoggerAnalyticsProvider);
        providers.add(promotedAnalyticsProvider);
        return providers;
    }

    @Provides
    @Named(TRACKING_DB)
    static PropellerDatabase provideTrackingDatabase(TrackingDbHelper dbHelper) {
        return new PropellerDatabase(dbHelper.getWritableDatabase());
    }

}
