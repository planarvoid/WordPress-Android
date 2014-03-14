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
    private final EventBus mEventBus;
    private final Context mContext;
    private final CollectionStorage mCollectionStorage;
    private final ActivitiesStorage mActivitiesStorage;
    private final UserAssociationStorage mUserAssociationStorage;
    private final PlaylistTagStorage mTagStorage;
    private final SoundRecorder mSoundRecorder;
    private final SyncStateManager mSyncStateManager;
    private final C2DMReceiver mC2DMReceiver;
    private final UnauthorisedRequestRegistry mUnauthorisedRequestRegistry;

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
        mEventBus = eventBus;
        mContext = context;
        mSyncStateManager = syncStateManager;
        mCollectionStorage = collectionStorage;
        mActivitiesStorage = activitiesStorage;
        mTagStorage = tagStorage;
        mUserAssociationStorage = userAssociationStorage;
        mSoundRecorder = soundRecorder;
        mC2DMReceiver = c2DMReceiver;
        mUnauthorisedRequestRegistry = unauthorisedRequestRegistry;
    }


    @Override
    public void call() {
        Log.d("AccountCleanup", "Purging user data...");

        mUnauthorisedRequestRegistry.clearObservedUnauthorisedRequestTimestamp();
        mSyncStateManager.clear();
        mCollectionStorage.clear();
        mActivitiesStorage.clear(null);
        mUserAssociationStorage.clear();
        mTagStorage.clear();

        mSoundRecorder.reset();

        FBToken.clear(mContext);

        mEventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
        mContext.sendBroadcast(new Intent(PlaybackService.Actions.RESET_ALL));


        mC2DMReceiver.unregister(mContext);
        FollowingOperations.clearState();
        ConnectionsCache.reset();
        SoundCloudApplication applicationContext = (SoundCloudApplication) mContext.getApplicationContext();
        applicationContext.clearLoggedInUser();
    }
}
