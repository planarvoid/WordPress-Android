package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.TxnResult;

import android.content.ContentValues;
import android.text.TextUtils;

import javax.inject.Inject;
import java.util.Collection;

public class PlaylistWriteStorage {

    private final PropellerDatabase propeller;

    @Inject
    public PlaylistWriteStorage(PropellerDatabase database) {
        this.propeller = database;
    }

    public TxnResult storePlaylists(Collection<ApiPlaylist> playlists) {
        return propeller.runTransaction(storePlaylistsTransaction(playlists));
    }

    private PropellerDatabase.Transaction storePlaylistsTransaction(final Collection<ApiPlaylist> playlists) {
        return new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ApiPlaylist playlist : playlists) {
                    step(propeller.upsert(Table.USERS.name, TableColumns.Users._ID, buildUserContentValues(playlist.getUser())));
                    step(propeller.upsert(Table.SOUNDS.name, TableColumns.Sounds._ID, buildPlaylistContentValues(playlist)));
                }
            }
        };
    }

    private ContentValues buildPlaylistContentValues(ApiPlaylist playlist) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Sounds._ID, playlist.getId())
                .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .put(TableColumns.Sounds.TITLE, playlist.getTitle())
                .put(TableColumns.Sounds.DURATION, playlist.getDuration())
                .put(TableColumns.Sounds.CREATED_AT, playlist.getCreatedAt().getTime())
                .put(TableColumns.Sounds.SHARING, playlist.getSharing().value())
                .put(TableColumns.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount())
                .put(TableColumns.Sounds.REPOSTS_COUNT, playlist.getStats().getRepostsCount())
                .put(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount())
                .put(TableColumns.Sounds.USER_ID, playlist.getUser().getId())
                .put(TableColumns.Sounds.TAG_LIST, TextUtils.join(" ", playlist.getTags()))
                .get();
    }

    private ContentValues buildUserContentValues(ApiUser user) {
        return ContentValuesBuilder.values()
                .put(TableColumns.Users._ID, user.getId())
                .put(TableColumns.Users.USERNAME, user.getUsername())
                .get();
    }

}
