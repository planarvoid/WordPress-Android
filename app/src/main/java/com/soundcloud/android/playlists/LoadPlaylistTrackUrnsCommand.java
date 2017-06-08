package com.soundcloud.android.playlists;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import javax.inject.Inject;
import java.util.List;

public class LoadPlaylistTrackUrnsCommand extends Command<Urn, List<Urn>> {

    private final PropellerDatabase database;

    @Inject
    public LoadPlaylistTrackUrnsCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call(Urn playlistUrn) {
        final Where whereTrackDataExists = filter()
                .whereEq(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID),
                         Tables.Sounds._ID)
                .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_TRACK);

        Query query = Query.from(Table.PlaylistTracks.name())
                           .innerJoin(Tables.Sounds.TABLE, whereTrackDataExists)
                           .select(field(TableColumns.PlaylistTracks.TRACK_ID).as(_ID))
                           .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId())
                           .order(TableColumns.PlaylistTracks.POSITION, Query.Order.ASC);
        return database.query(query).toList(new TrackUrnMapper());
    }

}
