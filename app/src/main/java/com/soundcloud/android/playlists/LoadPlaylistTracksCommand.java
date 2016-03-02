package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.POSITION;
import static com.soundcloud.propeller.query.Field.field;
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
        final String fullSoundIdColumn = Table.Sounds.field(TableColumns.Sounds._ID);
        return Query.from(Table.PlaylistTracks.name())
                .select(
                        field(fullSoundIdColumn).as(BaseColumns._ID),
                        TableColumns.Sounds.TITLE,
                        TableColumns.Sounds.USER_ID,
                        TableColumns.Users.USERNAME,
                        TableColumns.Sounds.SNIPPET_DURATION,
                        TableColumns.Sounds.FULL_DURATION,
                        TableColumns.Sounds.PLAYBACK_COUNT,
                        TableColumns.Sounds.LIKES_COUNT,
                        TableColumns.Sounds.SHARING,
                        Tables.TrackDownloads.REQUESTED_AT,
                        Tables.TrackDownloads.DOWNLOADED_AT,
                        Tables.TrackDownloads.UNAVAILABLE_AT,
                        TableColumns.TrackPolicies.BLOCKED,
                        TableColumns.TrackPolicies.SNIPPED,
                        TableColumns.TrackPolicies.SUB_MID_TIER,
                        TableColumns.TrackPolicies.SUB_HIGH_TIER,
                        Tables.TrackDownloads.REMOVED_AT,
                        Tables.OfflineContent._ID)

                .innerJoin(Table.Sounds.name(), Table.PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID), fullSoundIdColumn)
                .innerJoin(Table.Users.name(), Table.Sounds.field(TableColumns.Sounds.USER_ID), Table.Users.field(TableColumns.Users._ID))
                .leftJoin(Tables.TrackDownloads.TABLE.name(), fullSoundIdColumn, Tables.TrackDownloads._ID.qualifiedName())
                .innerJoin(Table.TrackPolicies.name(), fullSoundIdColumn, Table.TrackPolicies.field(TableColumns.TrackPolicies.TRACK_ID))
                .leftJoin(Tables.OfflineContent.TABLE, offlinePlaylistFilter())

                .whereEq(Table.Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereEq(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID), playlistUrn.getNumericId())
                .order(Table.PlaylistTracks.field(POSITION), ASC)
                .whereNull(Table.PlaylistTracks.field(TableColumns.PlaylistTracks.REMOVED_AT));
    }

    private Where offlinePlaylistFilter() {
        return filter()
                .whereEq(Tables.OfflineContent._ID, TableColumns.PlaylistTracks.PLAYLIST_ID)
                .whereEq(Tables.OfflineContent._TYPE, Tables.OfflineContent.TYPE_PLAYLIST);
    }
}
