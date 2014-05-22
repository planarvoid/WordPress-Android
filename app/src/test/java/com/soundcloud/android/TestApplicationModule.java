package com.soundcloud.android;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.api.http.RxHttpClient;
import com.soundcloud.android.api.http.json.JsonTransformer;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playback.widget.PlayerWidgetController;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.properties.ApplicationProperties;
import com.soundcloud.android.storage.PlaylistTagStorage;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import java.util.Collections;
import java.util.List;

@Module(injects = {SoundCloudApplication.class, TestApplication.class})
public class TestApplicationModule {

    private final SoundCloudApplication application;

    public TestApplicationModule(SoundCloudApplication application) {
        this.application = application;
    }

    @Provides
    public EventBus provideEventBus() {
        return mock(EventBus.class);
    }

    @Provides
    public ScModelManager provideModelManager() {
        return new ScModelManager(application);
    }

    @Provides
    public SharedPreferences provideSharedPreferences() {
        return application.getSharedPreferences("default", 0);
    }

    @Provides
    public AccountManager provideAccountManager() {
        return AccountManager.get(application);
    }

    @Provides
    public Context provideContext() {
        return application;
    }


    @Provides
    public Resources provideResources() {
        return application.getResources();
    }

    @Provides
    public ApplicationProperties provideAppProperties() {
        Resources mockResources = mock(Resources.class);
        when(mockResources.getString(R.string.build_type)).thenReturn("DEBUG");
        return new ApplicationProperties(mockResources);
    }

    @Provides
    public AnalyticsProperties provideAnalyticsProperties() {
        Resources mockResources = mock(Resources.class);
        when(mockResources.getBoolean(R.bool.analytics_enabled)).thenReturn(false);
        when(mockResources.getString(R.string.localytics_app_key)).thenReturn("123");
        return new AnalyticsProperties(mockResources);
    }

    @Provides
    public List<AnalyticsProvider> provideAnalyticsProviders() {
        return Collections.emptyList();
    }

    @Provides
    public ContentResolver provideContentResolver() {
        return application.getContentResolver();
    }

    @Provides
    public ImageOperations provideImageOperations() {
        return mock(ImageOperations.class);
    }

    @Provides
    public PlayerWidgetController provideWidgetController() {
        return mock(PlayerWidgetController.class);
    }

    @Provides
    public JsonTransformer provideJsonTransformer() {
        return mock(JsonTransformer.class);
    }

    @Provides
    public RxHttpClient provideRxHttpClient() {
        return mock(RxHttpClient.class);
    }

    @Provides
    public UnauthorisedRequestRegistry provideUnauthorizedRequestRegistry() {
        return mock(UnauthorisedRequestRegistry.class);
    }

    @Provides
    public SoundRecorder provideSoundRecorder() {
        return mock(SoundRecorder.class);
    }

    @Provides
    public PlaylistTagStorage providePlaylistTagStorage() {
        return mock(PlaylistTagStorage.class);
    }

    @Provides
    public IRemoteAudioManager provideIRemoteAudioManager() {
        return mock(IRemoteAudioManager.class);
    }
}
