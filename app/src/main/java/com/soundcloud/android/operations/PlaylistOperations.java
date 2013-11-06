package com.soundcloud.android.operations;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.dao.PlaylistStorage;
import com.soundcloud.android.dao.SoundAssociationStorage;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.SoundAssociation;
import com.soundcloud.android.model.User;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.ScContentProvider;
import com.soundcloud.android.service.sync.SyncStateManager;
import rx.Observable;
import rx.util.functions.Func1;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;

public class PlaylistOperations {

    private final PlaylistStorage mPlaylistStorage;
    private final SoundAssociationStorage mSoundAssocStorage;
    private final SyncStateManager mSyncStateManager;
    private final AccountOperations mAccountOperations;

    public PlaylistOperations(Context context) {
        this(new PlaylistStorage(), new SoundAssociationStorage(),
                new SyncStateManager(context), new AccountOperations(context));
    }

    @VisibleForTesting
    PlaylistOperations(PlaylistStorage playlistStorage, SoundAssociationStorage soundAssocStorage,
                              SyncStateManager syncStateManager, AccountOperations accountOperations) {
        this.mPlaylistStorage = playlistStorage;
        this.mSoundAssocStorage = soundAssocStorage;
        this.mSyncStateManager = syncStateManager;
        this.mAccountOperations = accountOperations;
    }

    public Observable<Playlist> createNewPlaylist(User currentUser, String title, boolean isPrivate, long firstTrackId) {
        // insert the new playlist into the database
        return mPlaylistStorage.createNewUserPlaylistAsync(currentUser, title, isPrivate, firstTrackId)
                .mapMany(handlePlaylistStored())
                .mapMany(handlePlaylistCreationStored());
    }

    private Func1<Playlist, Observable<SoundAssociation>> handlePlaylistStored() {
        return new Func1<Playlist, Observable<SoundAssociation>>() {
            @Override
            public Observable<SoundAssociation> call(Playlist playlist) {
                // store the newly created playlist as a sound association
                return mSoundAssocStorage.addCreationAsync(playlist);
            }
        };
    }

    private Func1<SoundAssociation, Observable<? extends Playlist>> handlePlaylistCreationStored() {
        final Account account = mAccountOperations.getSoundCloudAccount();
        return new Func1<SoundAssociation, Observable<? extends Playlist>>() {
            @Override
            public Observable<? extends Playlist> call(SoundAssociation soundAssociation) {
                // force to stale so we know to update the playlists next time it is viewed
                mSyncStateManager.forceToStale(Content.ME_PLAYLISTS);
                ContentResolver.requestSync(account, ScContentProvider.AUTHORITY, new Bundle());
                return Observable.just((Playlist) soundAssociation.getPlayable());
            }
        };
    }
}
