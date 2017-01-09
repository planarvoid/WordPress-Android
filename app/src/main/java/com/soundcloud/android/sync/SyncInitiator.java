package com.soundcloud.android.sync;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class SyncInitiator {

    private final Action0 requestSystemSyncAction = () -> requestSystemSync();

    public static final String ACTION_APPEND = ApiSyncService.ACTION_APPEND;
    public static final String ACTION_HARD_REFRESH = ApiSyncService.ACTION_HARD_REFRESH;

    private final Context context;
    private final AccountOperations accountOperations;

    @Inject
    SyncInitiator(Context context, AccountOperations accountOperations) {
        this.context = context;
        this.accountOperations = accountOperations;
    }

    public Action0 requestSystemSyncAction() {
        return requestSystemSyncAction;
    }

    public Observable<SyncJobResult> sync(Syncable syncable) {
        return getSyncObservable(createIntent(syncable));
    }

    public Observable<SyncJobResult> sync(Syncable syncable, String action) {
        return getSyncObservable(createIntent(syncable).setAction(action));
    }

    public Observable<SyncJobResult> syncTrack(Urn trackUrn) {
        return batchSyncTracks(Collections.singletonList(trackUrn));
    }

    public Observable<SyncJobResult> batchSyncTracks(Collection<Urn> trackUrns) {
        final Intent intent = createIntent(Syncable.TRACKS);
        SyncIntentHelper.putSyncEntities(intent, trackUrns);
        return getSyncObservable(intent);
    }

    public Observable<SyncJobResult> syncUser(Urn userUrn) {
        return batchSyncUsers(Collections.singletonList(userUrn));
    }

    public Observable<SyncJobResult> batchSyncUsers(List<Urn> userUrns) {
        final Intent intent = createIntent(Syncable.USERS);
        SyncIntentHelper.putSyncEntities(intent, userUrns);
        return getSyncObservable(intent);
    }

    public Observable<SyncJobResult> syncPlaylist(Urn playlistUrn) {
        if (playlistUrn.getNumericId() < 0) {
            return sync(Syncable.MY_PLAYLISTS);
        } else {
            final Intent intent = createIntent(Syncable.PLAYLIST);
            SyncIntentHelper.putSyncEntities(intent, Arrays.asList(playlistUrn));
            return getSyncObservable(intent);
        }

    }

    public Observable<SyncJobResult> batchSyncPlaylists(List<Urn> playlistUrns) {
        final Intent intent = createIntent(Syncable.PLAYLISTS);
        SyncIntentHelper.putSyncEntities(intent, playlistUrns);
        return getSyncObservable(intent);
    }

    public Observable<SyncJobResult> syncPlaylists(final Collection<Urn> playlists) {
        boolean syncMyPlaylists = false;
        List<Observable<SyncJobResult>> syncObservables = new ArrayList<>(playlists.size());
        for (Urn playlist : playlists) {
            if (playlist.isLocal()) {
                syncMyPlaylists = true;
            } else {
                syncObservables.add(syncPlaylist(playlist));
            }
        }

        if (syncMyPlaylists) {
            syncObservables.add(sync(Syncable.MY_PLAYLISTS));
        }
        return Observable.merge(syncObservables);
    }

    @NonNull
    private Observable<SyncJobResult> getSyncObservable(final Intent intent) {
        return Observable
                .create(new Observable.OnSubscribe<SyncJobResult>() {
                    @Override
                    public void call(Subscriber<? super SyncJobResult> subscriber) {
                        final ResultReceiverAdapter receiverAdapter = new ResultReceiverAdapter(subscriber,
                                                                                                Looper.getMainLooper());
                        context.startService(intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, receiverAdapter));
                    }
                });
    }


    public boolean requestSystemSync() {
        final Account soundCloudAccount = accountOperations.getSoundCloudAccount();
        if (soundCloudAccount != null) {
            ContentResolver.requestSync(soundCloudAccount, SyncConfig.AUTHORITY, new Bundle());
            return true;
        } else {
            return false;
        }
    }

    private Intent createIntent(Syncable syncable) {
        final Intent intent = new Intent(context, ApiSyncService.class);
        SyncIntentHelper.putSyncable(intent, syncable);
        intent.putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true);
        return intent;
    }



}
