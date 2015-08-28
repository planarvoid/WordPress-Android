package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.OfflineContentStorage.isOfflineLikesEnabledQuery;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.Table.TrackPolicies;
import static com.soundcloud.android.storage.Tables.OfflineContent;
import static com.soundcloud.propeller.query.ColumnFunctions.field;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.TrackUrnMapper;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.policies.PolicyOperations;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

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
                .whereLt(TrackPolicies.field(TableColumns.TrackPolicies.LAST_UPDATED), stalePolicyTimestamp)
                .orWhereNull(TrackPolicies.field(TableColumns.TrackPolicies.LAST_UPDATED));
    }

    static Query buildOfflineLikedTracksQuery() {
        final Where whereTrackDataExists = filter()
                .whereEq(Likes.field(TableColumns.Likes._ID), Sounds.field(TableColumns.Sounds._ID))
                .whereEq(Likes.field(TableColumns.Likes._TYPE), Sounds.field(TableColumns.Sounds._TYPE));

        final String likeId = Likes.field(TableColumns.Likes._ID);
        return Query.from(Likes.name())
                .select(
                        field(likeId).as(BaseColumns._ID))
                .innerJoin(Sounds.name(), whereTrackDataExists)
                .leftJoin(TrackPolicies.name(), likeId, TableColumns.TrackPolicies.TRACK_ID)
                .whereEq(Likes.field(TableColumns.Likes._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Likes.field(TableColumns.Likes.REMOVED_AT));
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
                .leftJoin(TrackPolicies.name(), trackIdFromPlaylistTracks, TrackPolicies.field(TableColumns.TrackPolicies.TRACK_ID));
    }
}
