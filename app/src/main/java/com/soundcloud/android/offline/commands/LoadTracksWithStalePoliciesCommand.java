package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.OfflineContent;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.TrackPolicies;
import static com.soundcloud.propeller.query.ColumnFunctions.field;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class LoadTracksWithStalePoliciesCommand extends Command<Boolean, Collection<Urn>, LoadTracksWithStalePoliciesCommand> {

    private final PropellerDatabase database;

    @Inject
    public LoadTracksWithStalePoliciesCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public Collection<Urn> call() throws Exception {
        final Collection<Urn> set = new TreeSet<>();
        if (input) {
            set.addAll(database.query(Query.from(Likes.name())
                    .select(field(Table.Likes + "." + TableColumns.Likes._ID).as(BaseColumns._ID))
                    .leftJoin(TrackPolicies.name(), Table.Likes + "." + _ID, TableColumns.TrackPolicies.TRACK_ID)
                    .where(TrackPolicies.name() + "." + TableColumns.TrackPolicies.LAST_UPDATED + " < ? OR " + TrackPolicies.name() + "." + TableColumns.TrackPolicies.LAST_UPDATED + " IS NULL", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)))
                    .toList(new UrnMapper()));
        }
        set.addAll(database.query(Query.from(PlaylistTracks.name())
                .select(field(Table.PlaylistTracks + "." + TableColumns.PlaylistTracks.TRACK_ID).as(BaseColumns._ID))
                .innerJoin(OfflineContent.name(), TableColumns.PlaylistTracks.PLAYLIST_ID, TableColumns.OfflineContent._ID)
                .leftJoin(TrackPolicies.name(), Table.PlaylistTracks + "." + TableColumns.PlaylistTracks.TRACK_ID, Table.TrackPolicies + "." + TableColumns.TrackPolicies.TRACK_ID)
                .where(TrackPolicies.name() + "." + TableColumns.TrackPolicies.LAST_UPDATED + " < ? OR " + TrackPolicies.name() + "." + TableColumns.TrackPolicies.LAST_UPDATED + " IS NULL", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)))
                .toList(new UrnMapper()));
        return set;
    }
}
