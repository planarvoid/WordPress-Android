package com.soundcloud.android.commands;

import com.soundcloud.android.playlists.PlaylistRecord;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.Iterables;
import com.soundcloud.propeller.ContentValuesBuilder;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.WriteResult;
import com.soundcloud.propeller.schema.BulkInsertValues;
import com.soundcloud.propeller.schema.Column;

import android.content.ContentValues;
import android.text.TextUtils;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

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
                step(propeller.bulkInsert(Tables.Sounds.TABLE, getPlaylistBulkValues(input)));
            }
        });
    }

    private static BulkInsertValues getPlaylistBulkValues(Iterable<? extends PlaylistRecord> input) {
        BulkInsertValues.Builder builder = new BulkInsertValues.Builder(getPlaylistColumns());
        for (PlaylistRecord playlist : input) {
            builder.addRow(buildPlaylistRow(playlist));
        }
        return builder.build();
    }

    public static List<Object> buildPlaylistRow(PlaylistRecord playlist) {
        return Arrays.asList(
                playlist.getUrn().getNumericId(),
                Tables.Sounds.TYPE_PLAYLIST,
                playlist.getTitle(),
                playlist.getDuration(),
                playlist.getCreatedAt().getTime(),
                playlist.getSharing().value(),
                playlist.getLikesCount(),
                playlist.getRepostsCount(),
                playlist.getTrackCount(),
                playlist.getUser().getUrn().getNumericId(),
                playlist.getGenre(),
                TextUtils.join(" ", playlist.getTags()),
                playlist.getPermalinkUrl(),
                playlist.getImageUrlTemplate().orNull(),
                playlist.isAlbum(),
                playlist.getSetType(),
                playlist.getReleaseDate()
        );
    }


    private static List<Column> getPlaylistColumns() {
        return Arrays.asList(
                Tables.Sounds._ID,
                Tables.Sounds._TYPE,
                Tables.Sounds.TITLE,
                Tables.Sounds.DURATION,
                Tables.Sounds.CREATED_AT,
                Tables.Sounds.SHARING,
                Tables.Sounds.LIKES_COUNT,
                Tables.Sounds.REPOSTS_COUNT,
                Tables.Sounds.TRACK_COUNT,
                Tables.Sounds.USER_ID,
                Tables.Sounds.GENRE,
                Tables.Sounds.TAG_LIST,
                Tables.Sounds.PERMALINK_URL,
                Tables.Sounds.ARTWORK_URL,
                Tables.Sounds.IS_ALBUM,
                Tables.Sounds.SET_TYPE,
                Tables.Sounds.RELEASE_DATE
        );

    }

    public static ContentValues buildPlaylistContentValues(PlaylistRecord playlist) {
        return ContentValuesBuilder.values()
                                   .put(Tables.Sounds._ID, playlist.getUrn().getNumericId())
                                   .put(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST)
                                   .put(Tables.Sounds.TITLE, playlist.getTitle())
                                   .put(Tables.Sounds.DURATION, playlist.getDuration())
                                   .put(Tables.Sounds.CREATED_AT, playlist.getCreatedAt().getTime())
                                   .put(Tables.Sounds.SHARING, playlist.getSharing().value())
                                   .put(Tables.Sounds.LIKES_COUNT, playlist.getLikesCount())
                                   .put(Tables.Sounds.REPOSTS_COUNT, playlist.getRepostsCount())
                                   .put(Tables.Sounds.TRACK_COUNT, playlist.getTrackCount())
                                   .put(Tables.Sounds.USER_ID, playlist.getUser().getUrn().getNumericId())
                                   .put(Tables.Sounds.GENRE, playlist.getGenre())
                                   .put(Tables.Sounds.TAG_LIST, TextUtils.join(" ", playlist.getTags()))
                                   .put(Tables.Sounds.PERMALINK_URL, playlist.getPermalinkUrl())
                                   .put(Tables.Sounds.ARTWORK_URL, playlist.getImageUrlTemplate().orNull())
                                   .put(Tables.Sounds.IS_ALBUM, playlist.isAlbum())
                                   .put(Tables.Sounds.SET_TYPE, playlist.getSetType())
                                   .put(Tables.Sounds.RELEASE_DATE, playlist.getReleaseDate())
                                   .get();

    }
}
