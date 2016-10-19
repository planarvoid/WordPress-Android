package com.soundcloud.android.commands;

import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;

import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StorePlaylistsCommand extends DefaultWriteStorageCommand<Iterable<? extends PlaylistRecord>, WriteResult> {

    private final StoreUsersCommand storeUsersCommand;

    @Inject
    public StorePlaylistsCommand(PropellerDatabase database, StoreUsersCommand storeUsersCommand) {
        super(database);
        this.storeUsersCommand = storeUsersCommand;
    }

    @Override
    protected WriteResult write(PropellerDatabase propeller, final Iterable<? extends PlaylistRecord> input) {
        return propeller.runTransaction(new PropellerDatabase.Transaction() {
            @Override
            public void steps(PropellerDatabase propeller) {
                step(storeUsersCommand.write(propeller, Iterables.transform(input, PlaylistRecord.TO_USER_RECORD)));
                step(propeller.bulkInsert_experimental(Table.Sounds, getPlaylistColumnTypes(), getPlaylistContentValues(input)));
            }
        });
    }

    @NonNull
    private List<ContentValues> getPlaylistContentValues(Iterable<? extends PlaylistRecord> input) {
        final List<ContentValues> playlistValues = new ArrayList<>(Iterables.size(input));
        for (PlaylistRecord playlist : input) {
            playlistValues.add(buildPlaylistContentValues(playlist));
        }
        return playlistValues;
    }

    public static ContentValues buildPlaylistContentValues(PlaylistRecord playlist) {
        return ContentValuesBuilder.values()
                                   .put(TableColumns.Sounds._ID, playlist.getUrn().getNumericId())
                                   .put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                                   .put(TableColumns.Sounds.TITLE, playlist.getTitle())
                                   .put(TableColumns.Sounds.DURATION, playlist.getDuration())
                                   .put(TableColumns.Sounds.CREATED_AT, playlist.getCreatedAt().getTime())
                                   .put(TableColumns.Sounds.SHARING, playlist.getSharing().value())
                                   .put(TableColumns.Sounds.LIKES_COUNT, playlist.getLikesCount())
                                   .put(TableColumns.Sounds.REPOSTS_COUNT, playlist.getRepostsCount())
                                   .put(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount())
                                   .put(TableColumns.Sounds.USER_ID, playlist.getUser().getUrn().getNumericId())
                                   .put(TableColumns.Sounds.TAG_LIST, TextUtils.join(" ", playlist.getTags()))
                                   .put(TableColumns.Sounds.PERMALINK_URL, playlist.getPermalinkUrl())
                                   .put(TableColumns.Sounds.ARTWORK_URL, playlist.getImageUrlTemplate().orNull())
                                   .put(TableColumns.Sounds.IS_ALBUM, playlist.isAlbum())
                                   .put(TableColumns.Sounds.SET_TYPE, playlist.getSetType())
                                   .put(TableColumns.Sounds.RELEASE_DATE, playlist.getReleaseDate())
                                   .get();
    }


    private Map<String, Class> getPlaylistColumnTypes() {
        final HashMap<String, Class> columns = new HashMap<>();
        columns.put(TableColumns.Sounds._ID, Long.class);
        columns.put(TableColumns.Sounds._TYPE, Integer.class);
        columns.put(TableColumns.Sounds.TITLE, String.class);
        columns.put(TableColumns.Sounds.DURATION, Long.class);
        columns.put(TableColumns.Sounds.CREATED_AT, Long.class);
        columns.put(TableColumns.Sounds.SHARING, String.class);
        columns.put(TableColumns.Sounds.LIKES_COUNT, Integer.class);
        columns.put(TableColumns.Sounds.REPOSTS_COUNT, Integer.class);
        columns.put(TableColumns.Sounds.TRACK_COUNT, Integer.class);
        columns.put(TableColumns.Sounds.USER_ID, Long.class);
        columns.put(TableColumns.Sounds.TAG_LIST, String.class);
        columns.put(TableColumns.Sounds.PERMALINK_URL, String.class);
        columns.put(TableColumns.Sounds.ARTWORK_URL, String.class);
        columns.put(TableColumns.Sounds.IS_ALBUM, Boolean.class);
        columns.put(TableColumns.Sounds.SET_TYPE, String.class);
        columns.put(TableColumns.Sounds.RELEASE_DATE, String.class);
        return columns;

    }
}
