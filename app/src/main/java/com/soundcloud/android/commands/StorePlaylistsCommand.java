package com.soundcloud.android.commands;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;
import android.text.TextUtils;

import javax.inject.Inject;

public class StorePlaylistsCommand extends StoreCommand<Iterable<ApiPlaylist>> {

    @Inject
    public StorePlaylistsCommand(PropellerDatabase database) {
        super(database);
    }

    public static ContentValues buildPlaylistContentValues(ApiPlaylist playlist) {
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
                .put(TableColumns.Sounds.PERMALINK_URL, playlist.getPermalinkUrl())
                .get();
    }

    @Override
    protected WriteResult store() {
        return database.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                for (ApiPlaylist playlist : input) {
                    step(propeller.upsert(Table.Users, StoreUsersCommand.buildUserContentValues(playlist.getUser())));
                    step(propeller.upsert(Table.Sounds, buildPlaylistContentValues(playlist)));
                }
            }
        });
    }
}
