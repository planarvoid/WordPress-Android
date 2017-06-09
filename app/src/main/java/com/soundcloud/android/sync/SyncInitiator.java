package com.soundcloud.android.sync;

import static com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.DefaultDisposableCompletableObserver;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
public class SyncInitiator {

    public static final String ACTION_APPEND = ApiSyncService.ACTION_APPEND;
    public static final String ACTION_HARD_REFRESH = ApiSyncService.ACTION_HARD_REFRESH;

    private final Context context;
    private final AccountOperations accountOperations;
    private final Scheduler scheduler;

    @Inject
    SyncInitiator(Context context, AccountOperations accountOperations, @Named(RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.context = context;
        this.accountOperations = accountOperations;
        this.scheduler = scheduler;
    }

    public Disposable syncAndForget(Syncable syncable) {
        return sync(syncable).toCompletable().subscribeWith(new DefaultDisposableCompletableObserver());
    }

    public Single<SyncJobResult> sync(Syncable syncable) {
        return getSyncObservable(createIntent(syncable));
    }

    public Single<SyncJobResult> sync(Syncable syncable, String action) {
        return getSyncObservable(createIntent(syncable).setAction(action));
    }

    public Single<SyncJobResult> syncTrack(Urn trackUrn) {
        return batchSyncTracks(Collections.singletonList(trackUrn));
    }

    public Single<SyncJobResult> batchSyncTracks(Collection<Urn> trackUrns) {
        final Intent intent = createIntent(Syncable.TRACKS);
        SyncIntentHelper.putSyncEntities(intent, trackUrns);
        return getSyncObservable(intent);
    }

    public Single<SyncJobResult> syncUser(Urn userUrn) {
        return batchSyncUsers(Collections.singletonList(userUrn));
    }

    public Single<SyncJobResult> batchSyncUsers(List<Urn> userUrns) {
        final Intent intent = createIntent(Syncable.USERS);
        SyncIntentHelper.putSyncEntities(intent, userUrns);
        return getSyncObservable(intent);
    }

    public Disposable syncPlaylistAndForget(Urn playlistUrn) {
        return syncPlaylist(playlistUrn).subscribeWith(new DefaultSingleObserver<>());
    }

    public Single<SyncJobResult> syncPlaylist(Urn playlistUrn) {
        if (playlistUrn.getNumericId() < 0) {
            return sync(Syncable.MY_PLAYLISTS);
        } else {
            final Intent intent = createIntent(Syncable.PLAYLIST);
            SyncIntentHelper.putSyncEntities(intent, Arrays.asList(playlistUrn));
            return getSyncObservable(intent);
        }
    }

    public Single<SyncJobResult> batchSyncPlaylists(List<Urn> playlistUrns) {
        final Intent intent = createIntent(Syncable.PLAYLISTS);
        SyncIntentHelper.putSyncEntities(intent, playlistUrns);
        return getSyncObservable(intent);
    }

    public void syncPlaylistsAndForget(final Collection<Urn> playlists) {
        syncPlaylists(playlists).subscribe(new DefaultObserver<>());
    }

    Observable<SyncJobResult> syncPlaylists(final Collection<Urn> playlists) {
        boolean syncMyPlaylists = false;
        List<Single<SyncJobResult>> syncSingles = new ArrayList<>(playlists.size());
        for (Urn playlist : playlists) {
            if (playlist.isLocal()) {
                syncMyPlaylists = true;
            } else {
                syncSingles.add(syncPlaylist(playlist));
            }
        }

        if (syncMyPlaylists) {
            syncSingles.add(sync(Syncable.MY_PLAYLISTS));
        }
        return Single.merge(syncSingles).toObservable();
    }

    @NonNull
    private Single<SyncJobResult> getSyncObservable(final Intent intent) {
        return Single.<SyncJobResult>create(subscriber -> {
            final ResultReceiverAdapter receiverAdapter = new ResultReceiverAdapter(subscriber,
                                                                                    Looper.getMainLooper());
            context.startService(intent.putExtra(ApiSyncService.EXTRA_STATUS_RECEIVER, receiverAdapter));
        }).observeOn(scheduler);
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
