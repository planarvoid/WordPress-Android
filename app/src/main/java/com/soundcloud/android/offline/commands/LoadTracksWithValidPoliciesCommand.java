package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand.buildOfflineLikedTracksQuery;
import static com.soundcloud.android.offline.commands.LoadTracksWithStalePoliciesCommand.buildOfflinePlaylistTracksQuery;
import static com.soundcloud.android.storage.Table.Likes;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;

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
            set.addAll(database.query(buildOfflineLikedTracksQuery()
                    .whereEq(TableColumns.TrackPolicies.SYNCABLE, true)
                    .order(Likes + "." + TableColumns.Likes.CREATED_AT, Query.ORDER_DESC))
                    .toList(new UrnMapper()));
        }
        set.addAll(database.query(buildOfflinePlaylistTracksQuery()
                .whereEq(TableColumns.TrackPolicies.SYNCABLE, true))
                .toList(new UrnMapper()));
        return set;
    }

}
