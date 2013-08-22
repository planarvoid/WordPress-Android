package com.soundcloud.android.accounts;

import static com.soundcloud.android.activity.auth.FacebookSSO.FBToken;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.exception.OperationFailedException;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.CollectionStorage;
import com.soundcloud.android.dao.UserAssociationStorage;
import com.soundcloud.android.operations.following.FollowingOperations;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.service.sync.SyncStateManager;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.Context;
import android.content.Intent;

class AccountRemovalFunction implements Func1<Observer<Void>, Subscription> {
    private final Context mContext;
    private final Account mSoundCloudAccount;
    private final CollectionStorage mCollectionStorage;
    private final ActivitiesStorage mActivitiesStorage;
    private final UserAssociationStorage mUserAssociationStorage;
    private final SoundRecorder mSoundRecorder;
    private final AccountManager mAccountManager;
    private final SyncStateManager mSyncStateManager;
    private final PlayQueueManager mPlayQueueManager;
    private final C2DMReceiver mC2DMReceiver;

    public AccountRemovalFunction(Account soundCloudAccount, AccountManager accountManager, Context context){
        this(soundCloudAccount, context, accountManager, new SyncStateManager(context), new CollectionStorage(context),new ActivitiesStorage(context),
                new UserAssociationStorage(), SoundRecorder.getInstance(context), new PlayQueueManager(context), new C2DMReceiver());
    }

    AccountRemovalFunction(Account soundCloudAccount, Context context, AccountManager accountManager, SyncStateManager syncStateManager,
                           CollectionStorage collectionStorage, ActivitiesStorage activitiesStorage, UserAssociationStorage userAssociationStorage,
                           SoundRecorder soundRecorder, PlayQueueManager playQueueManager, C2DMReceiver c2DMReceiver) {
        mSoundCloudAccount = soundCloudAccount;
        mContext = context;
        mAccountManager = accountManager;
        mSyncStateManager = syncStateManager;
        mCollectionStorage = collectionStorage;
        mActivitiesStorage = activitiesStorage;
        mUserAssociationStorage = userAssociationStorage;
        mSoundRecorder = soundRecorder;
        mPlayQueueManager = playQueueManager;
        mC2DMReceiver = c2DMReceiver;
    }



    @Override
    public Subscription call(Observer<Void> observer) {
        try {
            AccountManagerFuture<Boolean> accountRemovalFuture = mAccountManager.removeAccount(mSoundCloudAccount,null,null);

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

        mSyncStateManager.clear();
        mCollectionStorage.clear();
        mActivitiesStorage.clear(null);
        mUserAssociationStorage.clear();

        mSoundRecorder.reset();

        mPlayQueueManager.clearState();
        FBToken.clear(mContext);

        mContext.sendBroadcast(new Intent(Actions.LOGGING_OUT));
        mContext.sendBroadcast(new Intent(CloudPlaybackService.RESET_ALL));


        mC2DMReceiver.unregister(mContext);
        FollowingOperations.clearState();
        ConnectionsCache.reset();
        SoundCloudApplication applicationContext = (SoundCloudApplication)mContext.getApplicationContext();
        applicationContext.clearLoggedInUser();
    }

}
