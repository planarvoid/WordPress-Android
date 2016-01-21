package com.soundcloud.android.accounts;

import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.collection.CollectionOperations;
import com.soundcloud.android.commands.ClearTableCommand;
import com.soundcloud.android.configuration.PlanStorage;
import com.soundcloud.android.configuration.features.FeatureStorage;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.discovery.DiscoveryOperations;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.LegacyUserAssociationStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.stream.SoundStreamOperations;
import com.soundcloud.android.sync.SyncCleanupAction;
import com.soundcloud.android.sync.playlists.RemoveLocalPlaylistsCommand;
import com.soundcloud.android.utils.Log;
import com.soundcloud.propeller.PropellerWriteException;
import rx.functions.Action0;

import javax.inject.Inject;

class AccountCleanupAction implements Action0 {

    private static final String TAG = "AccountCleanup";

    private final LegacyUserAssociationStorage legacyUserAssociationStorage;
    //TODO: PlaylistTagStorage collaborator can be removed here once recommendations feature is enabled.
    private final PlaylistTagStorage tagStorage;
    private final SoundRecorder soundRecorder;
    private final FeatureStorage featureStorage;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;
    private final OfflineSettingsStorage offlineSettingsStorage;
    private final SyncCleanupAction syncCleanupAction;
    private final PlanStorage planStorage;
    private final RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand;
    private final DiscoveryOperations discoveryOperations;
    private final ClearTableCommand clearTableCommand;
    private final StationsOperations stationsOperations;
    private final CollectionOperations collectionOperations;
    private final SoundStreamOperations soundStreamOperations;

    @Inject
    AccountCleanupAction(LegacyUserAssociationStorage legacyUserAssociationStorage,
                         PlaylistTagStorage tagStorage, SoundRecorder soundRecorder, FeatureStorage featureStorage,
                         UnauthorisedRequestRegistry unauthorisedRequestRegistry,
                         OfflineSettingsStorage offlineSettingsStorage,
                         SyncCleanupAction syncCleanupAction,
                         PlanStorage planStorage, RemoveLocalPlaylistsCommand removeLocalPlaylistsCommand,
                         DiscoveryOperations discoveryOperations,
                         ClearTableCommand clearTableCommand,
                         StationsOperations stationsOperations,
                         CollectionOperations collectionOperations,
                         SoundStreamOperations soundStreamOperations) {
        this.tagStorage = tagStorage;
        this.legacyUserAssociationStorage = legacyUserAssociationStorage;
        this.soundRecorder = soundRecorder;
        this.featureStorage = featureStorage;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
        this.offlineSettingsStorage = offlineSettingsStorage;
        this.syncCleanupAction = syncCleanupAction;
        this.planStorage = planStorage;
        this.removeLocalPlaylistsCommand = removeLocalPlaylistsCommand;
        this.discoveryOperations = discoveryOperations;
        this.clearTableCommand = clearTableCommand;
        this.stationsOperations = stationsOperations;
        this.collectionOperations = collectionOperations;
        this.soundStreamOperations = soundStreamOperations;
    }

    @Override
    public void call() {
        Log.d(TAG, "Purging user data...");

        clearCollections();
        unauthorisedRequestRegistry.clearObservedUnauthorisedRequestTimestamp();
        legacyUserAssociationStorage.clear();
        tagStorage.clear();
        offlineSettingsStorage.clear();
        featureStorage.clear();
        syncCleanupAction.clear();
        planStorage.clear();
        soundRecorder.reset();
        stationsOperations.clearData();
        discoveryOperations.clearData();
        collectionOperations.clearData();
        soundStreamOperations.clearData();
    }

    private void clearCollections() {
        try {
            clearTableCommand.call(Table.Likes);
            clearTableCommand.call(Table.Posts);
            clearTableCommand.call(Table.SoundStream);
            clearTableCommand.call(Table.Activities);
            clearTableCommand.call(Table.Comments);
            clearTableCommand.call(Table.PromotedTracks);
            clearTableCommand.call(Table.Waveforms);
            removeLocalPlaylistsCommand.call(null);
        } catch (PropellerWriteException e) {
            Log.e(TAG, "Could not clear collections ", e);
        }
    }

}
