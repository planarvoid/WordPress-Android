package com.soundcloud.android.analytics;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.localytics.LocalyticsPushReceiver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;
import org.jetbrains.annotations.Nullable;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(addsTo = ApplicationModule.class, injects = {SoundCloudApplication.class, LocalyticsPushReceiver.class})
public class AnalyticsModule {

    public static final String TRACKING_DB = "TrackingDB";

    @Provides
    @Nullable
    ComScoreAnalyticsProvider provideComScoreProvider(Context context) {
        // cf. https://github.com/soundcloud/SoundCloud-Android/issues/1811
        try {
            return new ComScoreAnalyticsProvider(context);
        } catch (Exception e) {
            ErrorUtils.handleSilentException("Error during Comscore library init", e);
            return null;
        }
    }

    @Provides
    @Named(TRACKING_DB)
    PropellerDatabase provideTrackingDatabase(TrackingDbHelper dbHelper) {
        return new PropellerDatabase(dbHelper.getWritableDatabase());
    }

    @Provides
    @Singleton
    LocalyticsAmpSession provideLocalyticsSession(Context context, AnalyticsProperties analyticsProperties) {
        return new LocalyticsAmpSession(context, analyticsProperties.getLocalyticsAppKey());
    }

}
