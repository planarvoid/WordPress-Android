package com.soundcloud.android.analytics;

import static com.soundcloud.android.api.ApiModule.API_HTTP_CLIENT;

import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.analytics.eventlogger.EventLoggerAnalyticsProvider;
import com.soundcloud.android.analytics.promoted.PromotedAnalyticsProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerDatabase;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import org.jetbrains.annotations.Nullable;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Module
public class AnalyticsModule {

    public static final String TRACKING_DB = "TrackingDB";
    public static final String BASE_PROVIDERS = "BaseProviders";
    public static final String TRACKING_HTTP_CLIENT = "TrackingHttpClient";

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
    @Named(BASE_PROVIDERS)
    List<AnalyticsProvider> provideBaseProviders(EventLoggerAnalyticsProvider eventLoggerAnalyticsProvider,
                                                 PromotedAnalyticsProvider promotedAnalyticsProvider) {
        List<AnalyticsProvider> providers = new ArrayList<>(3);
        providers.add(eventLoggerAnalyticsProvider);
        providers.add(promotedAnalyticsProvider);
        return providers;
    }

    @Provides
    @Named(TRACKING_DB)
    PropellerDatabase provideTrackingDatabase(TrackingDbHelper dbHelper) {
        return new PropellerDatabase(dbHelper.getWritableDatabase());
    }

    @Provides
    @Singleton
    @Named(TRACKING_HTTP_CLIENT)
    public OkHttpClient provideOkHttpClient(OkHttpClient.Builder clientBuilder) {
        return clientBuilder.build();
    }
}
