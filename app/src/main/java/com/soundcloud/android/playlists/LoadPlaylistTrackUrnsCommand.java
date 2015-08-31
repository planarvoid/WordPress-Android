package com.soundcloud.android.playlists;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.List;

public class LoadPlaylistTrackUrnsCommand extends LegacyCommand<Urn, List<Urn>, LoadPlaylistTrackUrnsCommand> {

    private final PropellerDatabase database;

    @Inject
    public LoadPlaylistTrackUrnsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        final Where whereTrackDataExists = filter()
                .whereEq(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID), Table.Sounds.field(TableColumns.Sounds._ID))
                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK);

        Query query = Query.from(Table.PlaylistTracks.name())
                .innerJoin(Table.Sounds.name(), whereTrackDataExists)
                .select(field(TableColumns.PlaylistTracks.TRACK_ID).as(_ID))
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, input.getNumericId())
                .order(TableColumns.PlaylistTracks.POSITION, Query.Order.ASC);
        return database.query(query).toList(new TrackUrnMapper());
    }

}
