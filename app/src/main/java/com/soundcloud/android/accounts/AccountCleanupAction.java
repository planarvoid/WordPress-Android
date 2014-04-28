package com.soundcloud.android.accounts;

import static com.soundcloud.android.onboarding.auth.FacebookSSOActivity.FBToken;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.PlaylistTagStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.sync.SyncStateManager;
import com.soundcloud.android.utils.Log;
import rx.functions.Action0;

import android.content.Context;
import android.content.Intent;

class AccountCleanupAction implements Action0 {
    private final EventBus eventBus;
    private final Context context;
    private final CollectionStorage collectionStorage;
    private final ActivitiesStorage activitiesStorage;
    private final UserAssociationStorage userAssociationStorage;
    private final PlaylistTagStorage tagStorage;
    private final SoundRecorder soundRecorder;
    private final SyncStateManager syncStateManager;
    private final C2DMReceiver c2dmReceiver;
    private final UnauthorisedRequestRegistry unauthorisedRequestRegistry;

    public AccountCleanupAction(Context context) {
        this(SoundCloudApplication.fromContext(context).getEventBus(),
                context, new SyncStateManager(context), new CollectionStorage(context),
                new ActivitiesStorage(context), new UserAssociationStorage(context), new PlaylistTagStorage(context),
                SoundRecorder.getInstance(context),
                new C2DMReceiver(), UnauthorisedRequestRegistry.getInstance(context));
    }

    @VisibleForTesting
    protected AccountCleanupAction(EventBus eventBus, Context context, SyncStateManager syncStateManager,
                                   CollectionStorage collectionStorage, ActivitiesStorage activitiesStorage,
                                   UserAssociationStorage userAssociationStorage, PlaylistTagStorage tagStorage,
                                   SoundRecorder soundRecorder, C2DMReceiver c2DMReceiver,
                                   UnauthorisedRequestRegistry unauthorisedRequestRegistry) {
        this.eventBus = eventBus;
        this.context = context;
        this.syncStateManager = syncStateManager;
        this.collectionStorage = collectionStorage;
        this.activitiesStorage = activitiesStorage;
        this.tagStorage = tagStorage;
        this.userAssociationStorage = userAssociationStorage;
        this.soundRecorder = soundRecorder;
        c2dmReceiver = c2DMReceiver;
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
        SoundCloudApplication applicationContext = (SoundCloudApplication) context.getApplicationContext();
        //FIXME: this writes to a reference on a background thread that we only read on the UI thread (visibility)
        applicationContext.clearLoggedInUser();

        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
        context.sendBroadcast(new Intent(PlaybackService.Actions.RESET_ALL));
    }
}
