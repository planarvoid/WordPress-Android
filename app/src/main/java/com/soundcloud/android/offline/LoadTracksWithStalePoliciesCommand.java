package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.IsOfflineLikedTracksEnabledCommand.isOfflineLikesEnabledQuery;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Tables.OfflineContent;
import static com.soundcloud.propeller.query.Field.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.schema.Column;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.Collection;
import java.util.TreeSet;

class LoadTracksWithStalePoliciesCommand extends Command<Void, Collection<Urn>> {

    private final PropellerDatabase database;

    @Inject
    public LoadTracksWithStalePoliciesCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public Collection<Urn> call(Void ignored) {
        final Collection<Urn> set = new TreeSet<>();
        final Where stalePolicyCondition = stalePolicyCondition();
        if (isOfflineLikesEnabled()) {
            set.addAll(database.query(buildOfflineLikedTracksQuery()
                                              .where(stalePolicyCondition)).toList(new TrackUrnMapper()));

        }
        set.addAll(database.query(buildOfflinePlaylistTracksQuery()
                                          .where(stalePolicyCondition))
                           .toList(new TrackUrnMapper()));

        return set;
    }

    private boolean isOfflineLikesEnabled() {
        return database.query(isOfflineLikesEnabledQuery()).firstOrDefault(Boolean.class, false);
    }

    private Where stalePolicyCondition() {
        final long stalePolicyTimestamp = System.currentTimeMillis() - PolicyOperations.POLICY_STALE_AGE_MILLISECONDS;

        return filter()
                .whereLt(Tables.TrackPolicies.LAST_UPDATED, stalePolicyTimestamp)
                .orWhereNull(Tables.TrackPolicies.LAST_UPDATED);
    }

    static Query buildOfflineLikedTracksQuery() {
        final Where whereTrackDataExists = filter()
                .whereEq(Tables.Likes._ID, Tables.Sounds._ID)
                .whereEq(Tables.Likes._TYPE, Tables.Sounds._TYPE);

        final Column likeId = Tables.Likes._ID;
        return Query.from(Tables.Likes.TABLE)
                    .select(likeId.as(BaseColumns._ID))
                    .innerJoin(Tables.Sounds.TABLE, whereTrackDataExists)
                    .leftJoin(Tables.TrackPolicies.TABLE, likeId, Tables.TrackPolicies.TRACK_ID)
                    .whereEq(Tables.Likes._TYPE, Tables.Sounds.TYPE_TRACK)
                    .whereNull(Tables.Likes.REMOVED_AT);
    }

    static Query buildOfflinePlaylistTracksQuery() {
        final Where filterOfflinePlaylist = filter()
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, OfflineContent._ID.qualifiedName())
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);

        final String trackIdFromPlaylistTracks = PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID);
        return Query.from(PlaylistTracks.name())
                    .select(
                            field(trackIdFromPlaylistTracks).as(BaseColumns._ID))
                    .innerJoin(OfflineContent.TABLE.name(), filterOfflinePlaylist)
                    .leftJoin(Tables.TrackPolicies.TABLE.name(),
                              trackIdFromPlaylistTracks,
                              Tables.TrackPolicies.TRACK_ID.qualifiedName());
    }
}
