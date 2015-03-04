package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.OfflineContent;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.SoundView;
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
import java.util.LinkedHashSet;

public class LoadTracksWithValidPoliciesCommand extends Command<Boolean, Collection<Urn>, LoadTracksWithValidPoliciesCommand> {

    private final PropellerDatabase database;

    @Inject
    public LoadTracksWithValidPoliciesCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public Collection<Urn> call() throws Exception {
        final Collection<Urn> set = new LinkedHashSet<>();
        if (input) {
            set.addAll(database.query(Query.from(SoundView.name())
                    .select(field(Table.SoundView + "." + TableColumns.SoundView._ID).as(BaseColumns._ID))
                    .innerJoin(Likes.name(), Table.SoundView + "." + _ID, Table.Likes + "." + _ID)
                    .whereEq(TableColumns.SoundView.POLICIES_SYNCABLE, true)
                    .order(Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC))
                    .toList(new UrnMapper()));
        }
        set.addAll(database.query(Query.from(SoundView.name())
                .select(field(Table.SoundView + "." + TableColumns.SoundView._ID).as(BaseColumns._ID))
                .innerJoin(PlaylistTracks.name(), Table.SoundView + "." + _ID, Table.PlaylistTracks + "." + TableColumns.PlaylistTracks.TRACK_ID)
                .innerJoin(OfflineContent.name(), Table.PlaylistTracks + "." + TableColumns.PlaylistTracks.PLAYLIST_ID, Table.OfflineContent + "." + TableColumns.OfflineContent._ID)
                .whereEq(TableColumns.SoundView.POLICIES_SYNCABLE, true))
                .toList(new UrnMapper()));
        return set;
    }

}
