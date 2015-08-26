package com.soundcloud.android.accounts;

import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.LegacyUserAssociationStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.sync.playlists.RemoveLocalPlaylistsCommand;
import com.soundcloud.android.sync.stream.StreamSyncStorage;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropellerWriteException;
import rx.functions.Action0;

import javax.inject.Inject;

class AccountCleanupAction implements Action0 {

    private static final String TAG = "AccountCleanup";

    private final ActivitiesStorage activitiesStorage;
    private final LegacyUserAssociationStorage legacyUserAssociationStorage;
    private final PlaylistTagStorage tagStorage;
    private final SoundRecorder soundRecorder;
    private final SyncStateManager syncStateManager;
    private final FeatureStorage featureStorage;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final StreamSyncStorage streamSyncStorage;
    private final PlanStorage planStorage;
    private final RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand;
    private final ClearTableCommand clearTableCommand;
    private final StationsOperations stationsOperations;

    @Inject
    AccountCleanupAction(SyncStateManager syncStateManager,
                         ActivitiesStorage activitiesStorage, LegacyUserAssociationStorage legacyUserAssociationStorage,
                         PlaylistTagStorage tagStorage, SoundRecorder soundRecorder, FeatureStorage featureStorage,
                         UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                         OfflineSettingsStorage offlineSettingsStorage, StreamSyncStorage streamSyncStorage,
                         PlanStorage planStorage, RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand,
                         ClearTableCommand clearTableCommand, StationsOperations stationsOperations) {
        this.syncStateManager = syncStateManager;
        this.activitiesStorage = activitiesStorage;
        this.tagStorage = tagStorage;
        this.legacyUserAssociationStorage = legacyUserAssociationStorage;
        this.soundRecorder = soundRecorder;
        this.featureStorage = featureStorage;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.streamSyncStorage = streamSyncStorage;
        this.planStorage = planStorage;
        this.removeLocalPlaylistsCommand = removeLocalPlaylistsCommand;
        this.clearTableCommand = clearTableCommand;
        this.stationsOperations = stationsOperations;
    }

    @Override
    public void call() {
        Log.d(TAG, "Purging user data...");

        clearCollections();
        unauthorisedRequestRegistry.clearObservedUnauthorisedRequestTimestamp();
        syncStateManager.clear();
        activitiesStorage.clear(null);
        legacyUserAssociationStorage.clear();
        tagStorage.clear();
        offlineSettingsStorage.clear();
        featureStorage.clear();
        streamSyncStorage.clear();
        planStorage.clear();
        soundRecorder.reset();
        stationsOperations.clearData();
        FollowingOperations.clearState();
    }

    private void clearCollections()  {
        try {
            clearTableCommand.call(Table.Likes);
            clearTableCommand.call(Table.Posts);
            clearTableCommand.call(Table.SoundStream);
            clearTableCommand.call(Table.PromotedTracks);
            clearTableCommand.call(Table.Waveforms);
            removeLocalPlaylistsCommand.call(null);
        } catch (PropellerWriteException e) {
            Log.e(TAG, "Could not clear collections ", e);
        }
    }

}
