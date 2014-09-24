package com.soundcloud.android.accounts;

import static com.soundcloud.android.onboarding.auth.FacebookSSOActivity.FBToken;

import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.search.PlaylistTagStorage;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;
import rx.functions.Action0;

import android.content.Context;

import javax.inject.Inject;

class AccountCleanupAction implements Action0 {
    private final Context context;
    private final CollectionStorage collectionStorage;
    private final ActivitiesStorage activitiesStorage;
    private final UserAssociationStorage userAssociationStorage;
    private final PlaylistTagStorage tagStorage;
    private final SoundRecorder soundRecorder;
    private final SyncStateManager syncStateManager;
    private final C2DMReceiver c2dmReceiver;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;

    @Inject
    AccountCleanupAction(Context context, SyncStateManager syncStateManager,
                         CollectionStorage collectionStorage, ActivitiesStorage activitiesStorage,
                         UserAssociationStorage userAssociationStorage, PlaylistTagStorage tagStorage,
                         SoundRecorder soundRecorder, C2DMReceiver c2DMReceiver,
                         UnauthorisedRequestRegistry unauthorisedRequestRegistry) {
        this.context = context;
        this.syncStateManager = syncStateManager;
        this.collectionStorage = collectionStorage;
        this.activitiesStorage = activitiesStorage;
        this.tagStorage = tagStorage;
        this.userAssociationStorage = userAssociationStorage;
        this.soundRecorder = soundRecorder;
        this.c2dmReceiver = c2DMReceiver;
        this.unauthorisedRequestRegistry = unauthorisedRequestRegistry;
    }


    @Override
    public void call() {
        Log.d("AccountCleanup", "Purging user data...");

        unauthorisedRequestRegistry.clearObservedUnauthorisedRequestTimestamp();
        syncStateManager.clear();
        collectionStorage.clear();
        activitiesStorage.clear(null);
        userAssociationStorage.clear();
        tagStorage.clear();
        soundRecorder.reset();
        FBToken.clear(context);
        c2dmReceiver.unregister(context);
        FollowingOperations.clearState();
        ConnectionsCache.reset();
    }
}
