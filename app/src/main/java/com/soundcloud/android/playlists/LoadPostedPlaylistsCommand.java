package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Query.on;

import com.soundcloud.android.commands.PagedQueryCommand;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import javax.inject.Inject;

public class LoadPostedPlaylistsCommand extends PagedQueryCommand<ChronologicalQueryParams> {

    @Inject
    LoadPostedPlaylistsCommand(PropellerDatabase database) {
        super(database, new PostedPlaylistMapper());
    }

    @Override
    protected Query buildQuery(ChronologicalQueryParams input) {
        return Query.from(Table.SoundView.name())
                .select(
                        field(Table.SoundView.field(TableColumns.SoundView._ID)).as(TableColumns.SoundView._ID),
                        field(Table.SoundView.field(TableColumns.SoundView.TITLE)).as(TableColumns.SoundView.TITLE),
                        field(Table.SoundView.field(TableColumns.SoundView.USERNAME)).as(TableColumns.SoundView.USERNAME),
                        field(Table.SoundView.field(TableColumns.SoundView.TRACK_COUNT)).as(TableColumns.SoundView.TRACK_COUNT),
                        field(Table.SoundView.field(TableColumns.SoundView.LIKES_COUNT)).as(TableColumns.SoundView.LIKES_COUNT),
                        field(Table.SoundView.field(TableColumns.SoundView.SHARING)).as(TableColumns.SoundView.SHARING),
                        field(Table.SoundView.field(TableColumns.SoundView.CREATED_AT)).as(TableColumns.SoundView.CREATED_AT))
                .innerJoin(Table.Posts.name(),
                        on(Table.Posts.field(TableColumns.Posts.TARGET_ID), Table.SoundView.field(TableColumns.SoundView._ID))
                                .whereEq(Table.Posts.field(TableColumns.Posts.TARGET_TYPE), Table.SoundView.field(TableColumns.SoundView._TYPE)))
                .whereEq(Table.Posts.field(TableColumns.Posts.TYPE), TableColumns.Posts.TYPE_POST)
                .whereLt(Table.SoundView.field(TableColumns.SoundView.CREATED_AT), input.getTimestamp())
                .order(TableColumns.SoundView.CREATED_AT, Query.ORDER_DESC);
    }
}
