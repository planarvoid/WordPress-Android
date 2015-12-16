package com.soundcloud.android.analytics;

import static com.soundcloud.android.api.ApiModule.API_HTTP_CLIENT;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.comscore.ComScoreAnalyticsProvider;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.propeller.PropellerDatabase;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;
import org.jetbrains.annotations.Nullable;

import android.content.Context;

import javax.inject.Named;
import javax.inject.Singleton;

@Module(addsTo = ApplicationModule.class, injects = {SoundCloudApplication.class})
public class AnalyticsModule {

    public static final String TRACKING_DB = "TrackingDB";
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
    @Named(TRACKING_DB)
    PropellerDatabase provideTrackingDatabase(TrackingDbHelper dbHelper) {
        return new PropellerDatabase(dbHelper.getWritableDatabase());
    }

    @Provides
    @Singleton
    @Named(TRACKING_HTTP_CLIENT)
    public OkHttpClient provideOkHttpClient(@Named(API_HTTP_CLIENT) OkHttpClient apiHttpClient) {
        return apiHttpClient.clone();
    }
}
