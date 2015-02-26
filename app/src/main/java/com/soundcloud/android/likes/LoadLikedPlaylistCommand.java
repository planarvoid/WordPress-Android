package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LikedPlaylistMapper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.QueryResult;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;

public class LoadLikedPlaylistCommand extends Command<Urn, PropertySet, LoadLikedPlaylistCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedPlaylistCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public PropertySet call() throws Exception {
        final QueryResult queryResult = database.query(buildQuery(input));
        return queryResult.isEmpty() ? PropertySet.create() :
                new LikedPlaylistMapper().map(queryResult.iterator().next());
    }

    private Query buildQuery(Urn input) {
        return playlistLikeQuery().whereEq(Table.Likes + "." + TableColumns.Likes._ID, input.getNumericId());
    }

    static Query playlistLikeQuery() {
        return Query.from(Table.Likes.name(), Table.SoundView.name())
                .select(
                        field(Table.SoundView + "." + TableColumns.SoundView._ID).as(BaseColumns._ID),
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.SHARING,
                        field(Table.Likes + "." + TableColumns.Likes.CREATED_AT).as(TableColumns.Likes.CREATED_AT))
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .joinOn(Table.Likes + "." + TableColumns.Likes._ID, Table.SoundView + "." + TableColumns.Sounds._ID)
                .whereNull(TableColumns.Likes.REMOVED_AT);
    }

}
