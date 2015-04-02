package com.soundcloud.android.offline.commands;

import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.OfflineContent;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.TrackPolicies;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.LegacyCommand;
import com.soundcloud.android.commands.UrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Collection;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class LoadTracksWithStalePoliciesCommand extends LegacyCommand<Void, Collection<Urn>, LoadTracksWithStalePoliciesCommand> {

    private final PropellerDatabase database;
    private final OfflineSettingsStorage settingsStorage;

    @Inject
    public LoadTracksWithStalePoliciesCommand(PropellerDatabase database, OfflineSettingsStorage settingsStorage) {
        this.database = database;
        this.settingsStorage = settingsStorage;
    }

    @Override
    public Collection<Urn> call() throws Exception {
        final long stalePolicyTimestamp = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24);
        final Collection<Urn> set = new TreeSet<>();
        final Where stalePolicyCondition = filter()
                .whereLt(TrackPolicies.field(TableColumns.TrackPolicies.LAST_UPDATED), stalePolicyTimestamp)
                .orWhereNull(TrackPolicies.field(TableColumns.TrackPolicies.LAST_UPDATED));
        if (settingsStorage.isOfflineLikedTracksEnabled()) {
            set.addAll(database.query(buildOfflineLikedTracksQuery()
                    .where(stalePolicyCondition)).toList(new UrnMapper()));
        }
        set.addAll(database.query(buildOfflinePlaylistTracksQuery()
                .where(stalePolicyCondition))
                .toList(new UrnMapper()));
        return set;
    }

    static Query buildOfflineLikedTracksQuery() {
        final String likeId = Likes + "." + TableColumns.Likes._ID;
        return Query.from(Likes.name())
                .select(field(likeId).as(BaseColumns._ID))
                .leftJoin(TrackPolicies.name(), likeId, TableColumns.TrackPolicies.TRACK_ID)
                .whereEq(Likes.name() + "." + TableColumns.Likes._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Likes.name() + "." + TableColumns.Likes.REMOVED_AT);
    }

    static Query buildOfflinePlaylistTracksQuery() {
        final String trackIdFromPlaylistTracks = Table.PlaylistTracks + "." + TableColumns.PlaylistTracks.TRACK_ID;
        return Query.from(PlaylistTracks.name())
                .select(field(trackIdFromPlaylistTracks).as(BaseColumns._ID))
                .innerJoin(OfflineContent.name(), TableColumns.PlaylistTracks.PLAYLIST_ID, TableColumns.OfflineContent._ID)
                .leftJoin(TrackPolicies.name(), trackIdFromPlaylistTracks, Table.TrackPolicies + "." + TableColumns.TrackPolicies.TRACK_ID);
    }
}
