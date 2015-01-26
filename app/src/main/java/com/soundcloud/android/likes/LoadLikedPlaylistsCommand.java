package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.playlists.LikedPlaylistMapper;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;

public class LoadLikedPlaylistsCommand extends PagedQueryCommand<ChronologicalQueryParams> {

    @Inject
    LoadLikedPlaylistsCommand(PropellerDatabase database) {
        super(database, new LikedPlaylistMapper());
    }

    @Override
    protected Query buildQuery(ChronologicalQueryParams input) {
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
                .whereLt(Table.Likes + "." + TableColumns.Likes.CREATED_AT, input.getTimestamp())
                .joinOn(Table.Likes + "." + TableColumns.Likes._ID, Table.SoundView + "." + TableColumns.Sounds._ID)
                .order(Table.Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT);
    }


}
