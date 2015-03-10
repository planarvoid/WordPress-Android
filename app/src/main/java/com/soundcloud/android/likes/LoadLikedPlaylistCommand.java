package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.ColumnFunctions.count;
import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.LikedPlaylistMapper;
import com.soundcloud.android.playlists.PlaylistMapper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.query.WhereBuilder;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedPlaylistCommand extends Command<Urn, PropertySet, LoadLikedPlaylistCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedPlaylistCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public PropertySet call() throws Exception {
        final List<PropertySet> queryResult = database.query(buildQuery(input)).toList(new LikedPlaylistMapper());
        return queryResult.isEmpty() ? PropertySet.create() : queryResult.get(0);
    }

    private Query buildQuery(Urn input) {
        return playlistLikeQuery().whereEq(Table.Likes + "." + TableColumns.Likes._ID, input.getNumericId());
    }

    static Query playlistLikeQuery() {
        final Where likesSoundViewJoin = new WhereBuilder()
                .whereEq(Table.Likes.field(TableColumns.Likes._TYPE), Table.SoundView.field(TableColumns.SoundView._TYPE))
                .whereEq(Table.Likes.field(TableColumns.Likes._ID), Table.SoundView.field(TableColumns.SoundView._ID));

        return Query.from(Table.Likes.name())
                .select(
                        field(Table.SoundView + "." + TableColumns.SoundView._ID).as(BaseColumns._ID),
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.TRACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.SHARING,
                        count(TableColumns.PlaylistTracks.PLAYLIST_ID).as(PlaylistMapper.LOCAL_TRACK_COUNT),
                        field(Table.Likes + "." + TableColumns.Likes.CREATED_AT).as(TableColumns.Likes.CREATED_AT))

                .leftJoin(Table.PlaylistTracks.name(), Table.SoundView.field(TableColumns.SoundView._ID), TableColumns.PlaylistTracks.PLAYLIST_ID)
                .innerJoin(Table.SoundView.name(),likesSoundViewJoin)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereNull(Table.Likes.field(TableColumns.Likes.REMOVED_AT))
                .groupBy(Table.SoundView.field(TableColumns.SoundView._ID));
    }

}
