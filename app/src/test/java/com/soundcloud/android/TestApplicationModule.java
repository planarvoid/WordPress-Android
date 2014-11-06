package com.soundcloud.android;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.analytics.AnalyticsProviderFactory;
import com.soundcloud.android.analytics.localytics.LocalyticsPushReceiver;
import com.soundcloud.android.api.ApiScheduler;
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.service.PlaybackNotificationController;
import com.soundcloud.android.playback.service.managers.IRemoteAudioManager;
import com.soundcloud.android.playback.service.skippy.SkippyFactory;
import com.soundcloud.android.playback.widget.PlayerWidgetController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.ApiSyncer;
import com.soundcloud.android.tracks.TrackWriteStorage;
import com.soundcloud.propeller.rx.DatabaseScheduler;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;

import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;

// Purely needed to shut up Dagger, since all tests that use DefaultTestRunner go through
// Application#onCreate so injection has to be set up.
// Has no relevance for our newer tests that use SoundCloudTestRunner
@Module(injects = {SoundCloudApplication.class, TestApplication.class, ApiSyncer.class,
        LocalyticsPushReceiver.class, ApiSyncService.class})
public class TestApplicationModule {

    private final SoundCloudApplication application;

    public TestApplicationModule(SoundCloudApplication application) {
        this.application = application;
    }

    @Provides
    public EventBus provideEventBus() {
        return new TestEventBus();
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

    @Provides
    public PlaybackNotificationController providePlaybackNotificationController() {
        return mock(PlaybackNotificationController.class);
    }

    @Provides
    public TrackWriteStorage provideTrackWriteStorage() {
        return mock(TrackWriteStorage.class);
    }

    @Provides
    public DatabaseScheduler databaseScheduler() {
        return mock(DatabaseScheduler.class);
    }

    @Provides
    public SkippyFactory skippyFactory() {
        final SkippyFactory skippyFactory = mock(SkippyFactory.class);
        when(skippyFactory.create()).thenReturn(mock(Skippy.class));
        return skippyFactory;
    }

    @Provides
    public AnalyticsProviderFactory provideAnalyticsProviderFactory() {
        return mock(AnalyticsProviderFactory.class);
    }

    @Provides
    public ApiScheduler provideApiScheduler() {
        return mock(ApiScheduler.class);
    }

    @Provides
    public OkHttpClient provideOkHttpClient() {
        return new OkHttpClient();
    }

    @Provides
    public LocalyticsAmpSession provideLocalyticsSession() {
        return mock(LocalyticsAmpSession.class);
    }

    @Provides
    public SQLiteDatabase provideSqliteDatabase() {
        return mock(SQLiteDatabase.class);
    }
}

