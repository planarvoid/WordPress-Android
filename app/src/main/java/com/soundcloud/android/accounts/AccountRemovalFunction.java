package com.soundcloud.android.accounts;

import static com.soundcloud.android.onboarding.auth.FacebookSSOActivity.FBToken;
import static rx.Observable.OnSubscribeFunc;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.exception.OperationFailedException;
import com.soundcloud.android.api.UnauthorisedRequestRegistry;
import com.soundcloud.android.associations.FollowingOperations;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.creators.record.SoundRecorder;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.storage.ActivitiesStorage;
import com.soundcloud.android.storage.CollectionStorage;
import com.soundcloud.android.storage.UserAssociationStorage;
import com.soundcloud.android.sync.SyncStateManager;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;

class AccountRemovalFunction implements OnSubscribeFunc<Void> {
    private final Context mContext;
    private final Account mSoundCloudAccount;
    private final CollectionStorage mCollectionStorage;
    private final ActivitiesStorage mActivitiesStorage;
    private final UserAssociationStorage mUserAssociationStorage;
    private final SoundRecorder mSoundRecorder;
    private final AccountManager mAccountManager;
    private final SyncStateManager mSyncStateManager;
    private final C2DMReceiver mC2DMReceiver;
    private final UnauthorisedRequestRegistry mUnauthorisedRequestRegistry;

    public AccountRemovalFunction(Account soundCloudAccount, AccountManager accountManager, Context context) {
        this(soundCloudAccount, context, accountManager, new SyncStateManager(context), new CollectionStorage(context), new ActivitiesStorage(context),
                new UserAssociationStorage(context), SoundRecorder.getInstance(context), new C2DMReceiver(), UnauthorisedRequestRegistry.getInstance(context));
    }

    @VisibleForTesting
    protected AccountRemovalFunction(Account soundCloudAccount, Context context, AccountManager accountManager, SyncStateManager syncStateManager,
                           CollectionStorage collectionStorage, ActivitiesStorage activitiesStorage, UserAssociationStorage userAssociationStorage,
                           SoundRecorder soundRecorder, C2DMReceiver c2DMReceiver, UnauthorisedRequestRegistry unauthorisedRequestRegistry) {
        mSoundCloudAccount = soundCloudAccount;
        mContext = context;
        mAccountManager = accountManager;
        mSyncStateManager = syncStateManager;
        mCollectionStorage = collectionStorage;
        mActivitiesStorage = activitiesStorage;
        mUserAssociationStorage = userAssociationStorage;
        mSoundRecorder = soundRecorder;
        mC2DMReceiver = c2DMReceiver;
        mUnauthorisedRequestRegistry = unauthorisedRequestRegistry;
    }


    @Override
    public Subscription onSubscribe(Observer<? super Void> observer) {
        try {
            AccountManagerFuture<Boolean> accountRemovalFuture = mAccountManager.removeAccount(mSoundCloudAccount, null, null);

            if (accountRemovalFuture.getResult()) {
                finaliseAccountRemoval();
                observer.onCompleted();
            } else {
                observer.onError(new OperationFailedException());
            }

        } catch (Exception e) {
            observer.onError(e);
        }

        return Subscriptions.empty();
    }

    /**
     * TODO Should we just delete the private data directory? Context.getFilesDir()
     */
    private void finaliseAccountRemoval() {
        mUnauthorisedRequestRegistry.clearObservedUnauthorisedRequestTimestamp();
        mSyncStateManager.clear();
        mCollectionStorage.clear();
        mActivitiesStorage.clear(null);
        mUserAssociationStorage.clear();

        mSoundRecorder.reset();

        FBToken.clear(mContext);

        mContext.sendBroadcast(new Intent(Actions.LOGGING_OUT));
        mContext.sendBroadcast(new Intent(PlaybackService.Actions.RESET_ALL));


        mC2DMReceiver.unregister(mContext);
        FollowingOperations.clearState();
        ConnectionsCache.reset();
        SoundCloudApplication applicationContext = (SoundCloudApplication) mContext.getApplicationContext();
        applicationContext.clearLoggedInUser();
    }

}
