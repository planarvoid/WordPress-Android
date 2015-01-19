package com.soundcloud.android.likes;

import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropertySet;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class LoadLikedTracksCommand extends Command<Object, List<PropertySet>, LoadLikedTracksCommand> {

    private final PropellerDatabase database;

    @Inject
    LoadLikedTracksCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<PropertySet> call() throws Exception {
        return database.query(Query.from(Table.Likes.name(), Table.SoundView.name())
                .select(
                        field(Table.SoundView + "." + TableColumns.SoundView._ID).as(BaseColumns._ID),
                        TableColumns.SoundView.TITLE,
                        TableColumns.SoundView.USERNAME,
                        TableColumns.SoundView.DURATION,
                        TableColumns.SoundView.PLAYBACK_COUNT,
                        TableColumns.SoundView.LIKES_COUNT,
                        TableColumns.SoundView.SHARING)
                .whereEq(Table.Likes + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .joinOn(Table.Likes + "." + TableColumns.Likes._ID, Table.SoundView + "." + TableColumns.SoundView._ID)
                .order(Table.Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC)
                .whereNull(TableColumns.Likes.REMOVED_AT))
                .toList(new LikedTrackMapper());
    }
}
