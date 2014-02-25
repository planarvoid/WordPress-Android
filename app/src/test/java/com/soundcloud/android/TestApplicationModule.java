package com.soundcloud.android;

import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.AnalyticsProperties;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playback.service.PlayerWidgetController;
import com.soundcloud.android.properties.ApplicationProperties;
import dagger.Module;
import dagger.Provides;
import org.mockito.Mockito;

import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

@Module(injects = {SoundCloudApplication.class, TestApplication.class})
public class TestApplicationModule {

    private final SoundCloudApplication application;

    public TestApplicationModule(SoundCloudApplication application) {
        this.application = application;
    }

    @Provides
    public EventBus provideEventBus() {
        return Mockito.mock(EventBus.class);
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
    public ApplicationProperties provideAppProperties() {
        Resources mockResources = Mockito.mock(Resources.class);
        when(mockResources.getString(R.string.build_type)).thenReturn("DEBUG");
        return new ApplicationProperties(mockResources);
    }

    @Provides
    public AnalyticsProperties provideAnalyticsProperties() {
        Resources mockResources = Mockito.mock(Resources.class);
        when(mockResources.getBoolean(R.bool.analytics_enabled)).thenReturn(false);
        when(mockResources.getString(R.string.localytics_app_key)).thenReturn("123");
        return new AnalyticsProperties(mockResources);
    }

    @Provides
    public ImageOperations provideImageOperations() {
        return Mockito.mock(ImageOperations.class);
    }

    @Provides
    public PlayerWidgetController provideWidgetController() {
        return Mockito.mock(PlayerWidgetController.class);
    }
}
