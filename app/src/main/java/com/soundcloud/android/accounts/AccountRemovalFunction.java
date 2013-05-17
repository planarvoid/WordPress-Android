package com.soundcloud.android.accounts;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.exception.OperationFailedException;
import com.soundcloud.android.activity.auth.FacebookSSO;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.cache.ConnectionsCache;
import com.soundcloud.android.cache.FollowStatus;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.CollectionStorage;
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

public class AccountRemovalFunction implements Func1<Observer<Void>, Subscription> {
    private Account soundCloudAccount;
    private SoundCloudApplication soundCloudApplication;
    private CollectionStorage collectionStorage;
    private ActivitiesStorage activitiesStorage;
    private SoundRecorder soundRecorder;
    private AccountManager accountManager;
    private SyncStateManager syncStateManager;
    private PlayQueueManager playQueueManager;

    public AccountRemovalFunction(Account soundCloudAccount, AccountManager accountManager, Context context){
        this(soundCloudAccount, context, accountManager, new SyncStateManager(), new CollectionStorage(), new ActivitiesStorage(),
                SoundRecorder.getInstance(context), new PlayQueueManager(context));
    }

    AccountRemovalFunction(Account soundCloudAccount, Context context, AccountManager accountManager, SyncStateManager syncStateManager,
                           CollectionStorage collectionStorage, ActivitiesStorage activitiesStorage, SoundRecorder soundRecorder, PlayQueueManager playQueueManager) {
        this.soundCloudAccount = soundCloudAccount;
        this.soundCloudApplication = (SoundCloudApplication)context.getApplicationContext();
        this.accountManager = accountManager;
        this.syncStateManager = syncStateManager;
        this.collectionStorage = collectionStorage;
        this.activitiesStorage = activitiesStorage;
        this.soundRecorder = soundRecorder;
        this.playQueueManager = playQueueManager;
    }

    @Override
    public Subscription call(Observer<Void> observer) {
        try {
            AccountManagerFuture<Boolean> accountRemovalFuture = accountManager.removeAccount(soundCloudAccount,null,null);

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

    private void finaliseAccountRemoval() {
        syncStateManager.clear();
        collectionStorage.clear();
        activitiesStorage.clear(null);
        soundRecorder.reset();

        playQueueManager.clearState(soundCloudApplication);
        FacebookSSO.FBToken.clear(soundCloudApplication);

        soundCloudApplication.sendBroadcast(new Intent(Actions.LOGGING_OUT));
        soundCloudApplication.sendBroadcast(new Intent(CloudPlaybackService.RESET_ALL));

        C2DMReceiver.unregister(soundCloudApplication);
        FollowStatus.clearState();
        ConnectionsCache.reset();
        soundCloudApplication.clearLoggedInUser();
        soundCloudApplication.invalidateToken();
    }

}
