package com.soundcloud.android.accounts;

import static com.soundcloud.android.storage.StorageModule.FEATURES_FLAGS;

import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.collection.playhistory.PlayHistoryStorage;
import com.soundcloud.android.collection.recentlyplayed.RecentlyPlayedStorage;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.comments.CommentsStorage;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.deeplinks.ShortcutController;
import com.soundcloud.android.discovery.DiscoveryWritableStorage;
import com.soundcloud.android.gcm.GcmStorage;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.offline.SecureFileStorage;
import com.soundcloud.android.settings.notifications.NotificationPreferencesStorage;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.DatabaseManager;
import com.soundcloud.android.storage.PersistentStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.stream.StreamOperations;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsStorage;
import com.soundcloud.android.sync.SyncCleanupAction;
import com.soundcloud.android.sync.playlists.RemoveLocalPlaylistsCommand;
import com.soundcloud.android.users.FollowingStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.waveform.WaveformOperations;
import com.soundcloud.propeller.PropellerWriteException;
import rx.functions.Action0;

import javax.inject.Inject;
import javax.inject.Named;

class AccountCleanupAction implements Action0 {

    private static final String TAG = "AccountCleanup";

    private final FollowingStorage followingStorage;
    private final SoundRecorder soundRecorder;
    private final FeatureStorage featureStorage;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final SyncCleanupAction syncCleanupAction;
    private final PlanStorage planStorage;
    private final RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand;
    private final ClearTableCommand clearTableCommand;
    private final StationsOperations stationsOperations;
    private final CollectionOperations collectionOperations;
    private final StreamOperations streamOperations;
    private final ConfigurationOperations configurationOperations;
    private final NotificationPreferencesStorage notificationPreferencesStorage;
    private final PlayHistoryStorage playHistoryStorage;
    private final RecentlyPlayedStorage recentlyPlayedStorage;
    private final GcmStorage gcmStorage;
    private final PersistentStorage featureFlagsStorage;
    private final CommentsStorage commentsStorage;
    private final DatabaseManager databaseManager;
    private final SuggestedCreatorsStorage suggestedCreatorsStorage;
    private final ShortcutController shortcutController;
    private final SecureFileStorage secureFileStorage;
    private final DiscoveryWritableStorage discoveryWritableStorage;
    private final WaveformOperations waveformOperations;

    @Inject
    AccountCleanupAction(FollowingStorage followingStorage,
                         SoundRecorder soundRecorder, FeatureStorage featureStorage,
                         UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                         OfflineSettingsStorage offlineSettingsStorage,
                         SyncCleanupAction syncCleanupAction,
                         PlanStorage planStorage, RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand,
                         ClearTableCommand clearTableCommand,
                         StationsOperations stationsOperations,
                         CollectionOperations collectionOperations,
                         StreamOperations streamOperations,
                         ConfigurationOperations configurationOperations,
                         NotificationPreferencesStorage notificationPreferencesStorage,
                         PlayHistoryStorage playHistoryStorage,
                         RecentlyPlayedStorage recentlyPlayedStorage,
                         GcmStorage gcmStorage,
                         @Named(FEATURES_FLAGS) PersistentStorage featureFlagsStorage,
                         CommentsStorage commentsStorage,
                         DatabaseManager databaseManager,
                         SuggestedCreatorsStorage suggestedCreatorsStorage,
                         ShortcutController shortcutController,
                         SecureFileStorage secureFileStorage,
                         DiscoveryWritableStorage discoveryWritableStorage,
                         WaveformOperations waveformOperations) {
        this.followingStorage = followingStorage;
        this.soundRecorder = soundRecorder;
        this.featureStorage = featureStorage;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.syncCleanupAction = syncCleanupAction;
        this.planStorage = planStorage;
        this.removeLocalPlaylistsCommand = removeLocalPlaylistsCommand;
        this.clearTableCommand = clearTableCommand;
        this.stationsOperations = stationsOperations;
        this.collectionOperations = collectionOperations;
        this.streamOperations = streamOperations;
        this.configurationOperations = configurationOperations;
        this.notificationPreferencesStorage = notificationPreferencesStorage;
        this.playHistoryStorage = playHistoryStorage;
        this.recentlyPlayedStorage = recentlyPlayedStorage;
        this.gcmStorage = gcmStorage;
        this.featureFlagsStorage = featureFlagsStorage;
        this.commentsStorage = commentsStorage;
        this.databaseManager = databaseManager;
        this.suggestedCreatorsStorage = suggestedCreatorsStorage;
        this.shortcutController = shortcutController;
        this.secureFileStorage = secureFileStorage;
        this.discoveryWritableStorage = discoveryWritableStorage;
        this.waveformOperations = waveformOperations;
    }

    @Override
    public void call() {
        Log.d(TAG, "Purging user data...");

        clearCollections();
        unauthorisedRequestRegistry.clearObservedUnauthorisedRequestTimestamp();
        followingStorage.clear();
        offlineSettingsStorage.clear();
        secureFileStorage.reset();
        featureStorage.clear();
        syncCleanupAction.clear();
        planStorage.clear();
        soundRecorder.reset();
        stationsOperations.clearData();
        collectionOperations.clearData();
        streamOperations.clearData();
        suggestedCreatorsStorage.clear();
        configurationOperations.clearConfigurationSettings();
        notificationPreferencesStorage.clear();
        playHistoryStorage.clear();
        recentlyPlayedStorage.clear();
        gcmStorage.clearTokenForRefresh();
        featureFlagsStorage.clear();
        shortcutController.removeShortcuts();
        discoveryWritableStorage.clearData();

        // Note that the following call simply clears all the SQLite tables.
        // If necessary, shared preferences should be cleared manually.
        databaseManager.clearTables();
    }

    private void clearCollections() {
        try {
            clearTableCommand.call(Tables.Likes.TABLE);
            clearTableCommand.call(Tables.Posts.TABLE);
            clearTableCommand.call(Table.SoundStream);
            clearTableCommand.call(Table.Activities);
            commentsStorage.clear();
            clearTableCommand.call(Table.PromotedTracks);
            waveformOperations.clearWaveforms();
            clearTableCommand.call(Tables.TrackPolicies.TABLE);
            removeLocalPlaylistsCommand.call(null);
        } catch (PropellerWriteException e) {
            Log.e(TAG, "Could not clear collections ", e);
        }
    }

}
