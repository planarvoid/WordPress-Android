package com.soundcloud.android;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.ads.AdIdHelper;
import com.soundcloud.android.analytics.AnalyticsProviderFactory;
import com.soundcloud.android.analytics.localytics.LocalyticsPushReceiver;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.api.json.JsonTransformer;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.cast.CastConnectionHelper;
import com.soundcloud.android.cast.CastSessionController;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlaybackStrategy;
import com.soundcloud.android.playback.notification.PlaybackNotificationController;
import com.soundcloud.android.playback.IRemoteAudioManager;
import com.soundcloud.android.playback.skippy.SkippyFactory;
import com.soundcloud.android.playback.widget.PlayerWidgetController;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.skippy.Skippy;
import com.soundcloud.android.storage.StorageModule;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.ApiSyncer;
import com.soundcloud.android.sync.entities.EntitySyncJob;
import com.soundcloud.android.sync.entities.EntitySyncModule;
import com.soundcloud.android.sync.likes.LikesSyncModule;
import com.soundcloud.android.sync.likes.LikesSyncer;
import com.soundcloud.android.sync.posts.MyPlaylistsSyncer;
import com.soundcloud.android.sync.posts.PostsSyncModule;
import com.soundcloud.android.sync.posts.PostsSyncer;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.squareup.okhttp.OkHttpClient;
import dagger.Module;
import dagger.Provides;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;

import javax.inject.Named;

// Purely needed to shut up Dagger, since all tests that use DefaultTestRunner go through
// Application#onCreate so injection has to be set up.
// Has no relevance for our newer tests that use SoundCloudTestRunner
@Module(injects = {SoundCloudApplication.class, TestApplication.class, ApiSyncer.class,
        LocalyticsPushReceiver.class, ApiSyncService.class}, library = true)
public class TestApplicationModule {

    private final SoundCloudApplication application;

    public TestApplicationModule(SoundCloudApplication application) {
        this.application = application;
        this.application.attachBaseContext(application);
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

    @Provides
    @Named(StorageModule.DEVICE_MANAGEMENT)
    public SharedPreferences provideDeviceManagementPrefs(){
        return provideSharedPreferences();
    }

    @Provides
    @Named(StorageModule.DEVICE_KEYS)
    public SharedPreferences provideKeyPrefs(){
        return provideSharedPreferences();
    }

    @Provides
    @Named(StorageModule.OFFLINE_SETTINGS)
    public SharedPreferences provideOfflinePrefs() {
        return provideSharedPreferences();
    }

    @Provides
    @Named(StorageModule.FEATURES)
    public SharedPreferences provideFeatures() {
        return provideSharedPreferences();
    }

    @Provides
    @Named(StorageModule.STREAM_SYNC)
    public SharedPreferences provideStreamSync() {
        return provideSharedPreferences();
    }

    @Provides
    @Named(ApplicationModule.HIGH_PRIORITY)
    public Scheduler provideHighPrioScheduler() {
        return Schedulers.immediate();
    }

    @Provides
    @Named(ApplicationModule.LOW_PRIORITY)
    public Scheduler provideLowPrioScheduler() {
        return Schedulers.immediate();
    }

    @Provides
    public CastSessionController provideCastSessionReconnector() {
        return mock(CastSessionController.class);
    }

    @Provides
    public VideoCastManager provideVideoCastManager() {
        return mock(VideoCastManager.class);
    }

    @Provides
    public PlaybackStrategy providePlaybackStrategy() {
        return mock(PlaybackStrategy.class);
    }

    @Provides
    @Named(LikesSyncModule.TRACK_LIKES_SYNCER)
    LikesSyncer<ApiTrack> provideTrackLikesSyncer() {
        return mock(LikesSyncer.class);
    }

    @Provides
    @Named(LikesSyncModule.PLAYLIST_LIKES_SYNCER)
    LikesSyncer<ApiPlaylist> providePlaylistLikesSyncer() {
        return mock(LikesSyncer.class);
    }

    @Provides
    @Named(EntitySyncModule.TRACKS_SYNC)
    EntitySyncJob provideTracksSyncJob() {
        return mock(EntitySyncJob.class);
    }

    @Provides
    @Named(EntitySyncModule.PLAYLISTS_SYNC)
    EntitySyncJob providePlaylistsSyncJob() {
        return mock(EntitySyncJob.class);
    }

    @Provides
    @Named(EntitySyncModule.USERS_SYNC)
    EntitySyncJob provideUsersSyncJob() {
        return mock(EntitySyncJob.class);
    }

    @Provides
    public NetworkConnectionHelper provideNetworkConnectionHelper() {
        return mock(NetworkConnectionHelper.class);
    }

    @Provides
    NotificationManager provideNotificationManager() {
        return mock(NotificationManager.class);
    }

    @Provides
    MyPlaylistsSyncer provideMyPlaylistsSyncer() {
        return mock(MyPlaylistsSyncer.class);
    }

    @Provides
    @Named(PostsSyncModule.MY_TRACK_POSTS_SYNCER)
    PostsSyncer provideMyTrackPostsSyncer() {
        return mock(PostsSyncer.class);
    }

    @Provides
    @Named(PostsSyncModule.MY_PLAYLIST_POSTS_SYNCER)
    PostsSyncer provideMyPlaylistPostsSyncer() {
        return mock(PostsSyncer.class);
    }

    @Provides
    AdIdHelper provideAdIdHelper() {
        return mock(AdIdHelper.class);
    }

    @Provides
    CastConnectionHelper provideCastConnectionHelper() {
        return mock(CastConnectionHelper.class);
    }

}

