package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.POSITION;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.ASC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

import android.provider.BaseColumns;

import javax.inject.Inject;
import java.util.List;

public class LoadPlaylistTracksCommand extends Command<Urn, List<PropertySet>> {
    private final PropellerDatabase propeller;

    @Inject
    LoadPlaylistTracksCommand(PropellerDatabase propeller) {
        this.propeller = propeller;
    }

    @Override
    public List<PropertySet> call(Urn playlistUrn) {
        return propeller.query(getPlaylistTracksQuery(playlistUrn)).toList(new PlaylistTrackItemMapper());
    }

    private Query getPlaylistTracksQuery(Urn playlistUrn) {
        return Query.from(Table.PlaylistTracks.name())
                    .select(
                            Tables.Sounds._ID.as(BaseColumns._ID),
                            Tables.Sounds.TITLE,
                            Tables.Sounds.ARTWORK_URL,
                            Tables.Sounds.USER_ID,
                            Tables.Users.USERNAME,
                            Tables.Sounds.SNIPPET_DURATION,
                            Tables.Sounds.FULL_DURATION,
                            Tables.Sounds.PLAYBACK_COUNT,
                            Tables.Sounds.LIKES_COUNT,
                            Tables.Sounds.SHARING,
                            Tables.TrackDownloads.REQUESTED_AT,
                            Tables.TrackDownloads.DOWNLOADED_AT,
                            Tables.TrackDownloads.UNAVAILABLE_AT,
                            Tables.TrackPolicies.BLOCKED,
                            Tables.TrackPolicies.SNIPPED,
                            Tables.TrackPolicies.SUB_MID_TIER,
                            Tables.TrackPolicies.SUB_HIGH_TIER,
                            Tables.TrackDownloads.REMOVED_AT,
                            Tables.OfflineContent._ID)

                    .innerJoin(Tables.Sounds.TABLE.name(),
                               Table.PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID),
                               Tables.Sounds._ID.qualifiedName())
                    .innerJoin(Tables.Users.TABLE,
                               Tables.Sounds.USER_ID,
                               Tables.Users._ID)
                    .leftJoin(Tables.TrackDownloads.TABLE.name(),
                              Tables.Sounds._ID.qualifiedName(),
                              Tables.TrackDownloads._ID.qualifiedName())
                    .innerJoin(Tables.TrackPolicies.TABLE.name(),
                               Tables.Sounds._ID.qualifiedName(),
                               Tables.TrackPolicies.TRACK_ID.qualifiedName())
                    .leftJoin(Tables.OfflineContent.TABLE, offlinePlaylistFilter())

                    .whereEq(Tables.Sounds._TYPE, Tables.Sounds.TYPE_TRACK)
                    .whereEq(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID),
                             playlistUrn.getNumericId())
                    .order(Table.PlaylistTracks.field(POSITION), ASC)
                    .whereNull(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.REMOVED_AT));
    }

    private Where offlinePlaylistFilter() {
        return filter()
                .whereEq(Tables.OfflineContent._ID, TableColumns.PlaylistTracks.PLAYLIST_ID)
                .whereEq(Tables.OfflineContent._TYPE, Tables.OfflineContent.TYPE_PLAYLIST);
    }
}
