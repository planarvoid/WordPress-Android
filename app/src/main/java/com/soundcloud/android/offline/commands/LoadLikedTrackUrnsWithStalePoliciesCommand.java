package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Likes;
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
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadLikedTrackUrnsWithStalePoliciesCommand extends Command<Object, List<Urn>, LoadLikedTrackUrnsWithStalePoliciesCommand> {

    private final PropellerDatabase database;

    @Inject
    public LoadLikedTrackUrnsWithStalePoliciesCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public List<Urn> call() throws Exception {
        return database.query(Query.from(Likes.name())
                .select(field(Table.Likes + "." + TableColumns.Likes._ID).as(BaseColumns._ID))
                .leftJoin(TrackPolicies.name(), Table.Likes + "." + _ID, TableColumns.TrackPolicies.TRACK_ID)
                .where(TrackPolicies.name() + "." + TableColumns.TrackPolicies.LAST_UPDATED + " < ? OR " + TrackPolicies.name() + "." + TableColumns.TrackPolicies.LAST_UPDATED + " IS NULL", System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24)))
                .toList(new UrnMapper());
    }
}
