package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.exists;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;

class LoadPlaylistCommand extends LegacyCommand<Urn, PropertySet, LoadPlaylistCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadPlaylistCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public PropertySet call() throws Exception {
        final QueryResult queryResult = database.query(buildQuery(input));
        return queryResult.firstOrDefault(new PlaylistInfoMapper(), PropertySet.create());
    }

    private Query buildQuery(Urn input) {
        return Query.from(Table.SoundView.name())
                .select(
                        TableColumns.SoundView._ID,
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.USER_ID,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.REPOSTS_COUNT,
                        TableColumns.SoundView.PERMALINK_URL,
                        TableColumns.SoundView.SHARING,
                        TableColumns.SoundView.CREATED_AT,
                        count(TableColumns.PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        exists(likeQuery()).as(TableColumns.SoundView.USER_LIKE),
                        exists(repostQuery()).as(TableColumns.SoundView.USER_REPOST),
                        exists(isMarkedForOfflineQuery()).as(PlaylistMapper.IS_MARKED_FOR_OFFLINE)
                )
                .whereEq(TableColumns.SoundView._ID, input.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .leftJoin(Table.PlaylistTracks.name(), Table.SoundView.field(TableColumns.SoundView._ID), TableColumns.PlaylistTracks.PLAYLIST_ID)
                .groupBy(Table.SoundView.field(TableColumns.SoundView._ID));
    }

    private Query likeQuery() {
        final Where joinConditions = filter()
                .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), Table.Likes.field(TableColumns.Likes._ID))
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), Table.Likes.field(TableColumns.Likes._TYPE));

        return Query.from(Table.Likes.name())
                .innerJoin(Table.Sounds.name(), joinConditions)
                .whereEq(Table.Sounds.field(TableColumns.Sounds._ID), input.getNumericId())
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(TableColumns.Likes.REMOVED_AT);
    }

    private Query repostQuery() {
        final Where joinConditions = filter()
                .whereEq(TableColumns.Sounds._ID, TableColumns.Posts.TARGET_ID)
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Posts.TARGET_TYPE);

        return Query.from(Table.Posts.name())
                .innerJoin(Table.Sounds.name(), joinConditions)
                .whereEq(TableColumns.Sounds._ID, input.getNumericId())
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST);
    }

    private Query isMarkedForOfflineQuery() {
        return Query.from(Table.OfflineContent.name(), Table.Sounds.name())
                .joinOn(Table.SoundView + "." + TableColumns.SoundView._ID, Table.OfflineContent.name() + "." + TableColumns.Likes._ID)
                .whereEq(Table.OfflineContent + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
    }

}