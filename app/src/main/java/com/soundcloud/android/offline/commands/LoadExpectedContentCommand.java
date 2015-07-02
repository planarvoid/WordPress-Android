package com.soundcloud.android.offline.commands;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.OfflineContent;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.Table.TrackPolicies;
import static com.soundcloud.propeller.query.Filter.filter;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.soundcloud.android.commands.Command;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadRequest;
import com.soundcloud.android.offline.OfflineSettingsStorage;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.rx.RxResultMapper;

import javax.inject.Inject;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoadExpectedContentCommand extends Command<Void, Collection<DownloadRequest>> {

    private final PropellerDatabase database;
    private final OfflineSettingsStorage settingsStorage;

    private final Function<DownloadRequest.Builder, DownloadRequest> toDownloadRequest = new Function<DownloadRequest.Builder, DownloadRequest>() {
        @Override
        public DownloadRequest apply(DownloadRequest.Builder builder) {
            return builder.build();
        }
    };

    @Inject
    LoadExpectedContentCommand(PropellerDatabase database, OfflineSettingsStorage settingsStorage) {
        this.database = database;
        this.settingsStorage = settingsStorage;
    }

    @Override
    public Collection<DownloadRequest> call(Void ignored) {

        final List<OfflineRequestData> requestsData = new LinkedList<>();
        requestsData.addAll(tracksFromOfflinePlaylists());
        if (settingsStorage.isOfflineLikedTracksEnabled()) {
            requestsData.addAll(tracksFromLikes());
        }

        return Collections2.transform(getAggregatedRequestData(requestsData), toDownloadRequest);
    }

    private Collection<DownloadRequest.Builder> getAggregatedRequestData(List<OfflineRequestData> requestsData) {
        final LinkedHashMap<Urn, DownloadRequest.Builder> trackToRequestsDataMap = new LinkedHashMap<>();
        for (OfflineRequestData data : requestsData) {
            if (!trackToRequestsDataMap.containsKey(data.track)) {
                trackToRequestsDataMap.put(data.track, new DownloadRequest.Builder(data.track, data.duration));
            }
            trackToRequestsDataMap.get(data.track)
                    .addToPlaylist(data.playlist)
                    .addToLikes(data.isInLikes);
        }
        return trackToRequestsDataMap.values();
    }

    private List<OfflineRequestData> tracksFromLikes() {
        final Query likesToDownload = Query.from(Sounds.name())
                .select(
                        Sounds.field(_ID),
                        Sounds.field(TableColumns.Sounds.DURATION))
                .innerJoin(TrackPolicies.name(),
                        Likes.field(TableColumns.Likes._ID), TableColumns.TrackPolicies.TRACK_ID)
                .innerJoin(Table.Likes.name(),
                        Table.Likes.field(TableColumns.Likes._ID), Sounds.field(TableColumns.Sounds._ID))
                .where(isDownloadable())
                .whereEq(Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_TRACK)
                .whereNull(Likes.field(TableColumns.Likes.REMOVED_AT))
                .order(Table.Likes.field(TableColumns.Likes.CREATED_AT), Query.ORDER_DESC);

        return database.query(likesToDownload).toList(new LikedTrackMapper());
    }

    private Where isDownloadable() {
        return filter()
                .whereEq(TrackPolicies.field(TableColumns.TrackPolicies.SYNCABLE), true)
                .whereGt(TrackPolicies.field(TableColumns.TrackPolicies.LAST_UPDATED), System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30));
    }

    private List<OfflineRequestData> tracksFromOfflinePlaylists() {
        final Query orderedPlaylists = Query.from(OfflineContent.name())
                .select(
                        OfflineContent.field(TableColumns.OfflineContent._ID))
                .innerJoin(Sounds.name(), filter().whereEq(Sounds.field(_ID), OfflineContent.field(_ID)))
                .whereEq(Sounds.field(TableColumns.Sounds._TYPE), TableColumns.Sounds.TYPE_PLAYLIST)
                .order(Sounds.field(TableColumns.Sounds.CREATED_AT), Query.ORDER_DESC);

        final List<Long> playlistIds = database.query(orderedPlaylists).toList(RxResultMapper.scalar(Long.class));

        final Query playlistTracksToDownload = Query.from(PlaylistTracks.name())
                .select(
                        Sounds.field(_ID),
                        Sounds.field(TableColumns.Sounds.DURATION),
                        PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID))
                .innerJoin(Sounds.name(), filter()
                        .whereEq(Sounds.field(_ID), PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID))
                        .whereIn(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistIds))
                .innerJoin(TrackPolicies.name(),
                        PlaylistTracks.field(TableColumns.PlaylistTracks.TRACK_ID),
                        TrackPolicies.field(TableColumns.TrackPolicies.TRACK_ID))
                .where(isDownloadable())
                .whereNull(PlaylistTracks.field(TableColumns.PlaylistTracks.REMOVED_AT))
                .order(PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID), Query.ORDER_DESC)
                .order(PlaylistTracks.field(TableColumns.PlaylistTracks.POSITION), Query.ORDER_ASC);

        return database.query(playlistTracksToDownload).toList(new PlaylistTrackMapper());
    }

    private static class OfflineRequestData {
        private final Urn track;
        private final long duration;
        private final Urn playlist;
        private final boolean isInLikes;

        public OfflineRequestData(long trackId, long duration, long playlistId) {
            this.duration = duration;
            this.track = Urn.forTrack(trackId);
            this.playlist = Urn.forPlaylist(playlistId);
            this.isInLikes = false;
        }

        public OfflineRequestData(long trackId, long duration, boolean isInLikes) {
            this.duration = duration;
            this.track = Urn.forTrack(trackId);
            this.playlist = Urn.NOT_SET;
            this.isInLikes = isInLikes;
        }
    }

    private static class PlaylistTrackMapper implements ResultMapper<OfflineRequestData> {
        @Override
        public OfflineRequestData map(CursorReader reader) {
            return new OfflineRequestData(
                    reader.getLong(_ID),
                    reader.getLong(TableColumns.Sounds.DURATION),
                    reader.getLong(TableColumns.PlaylistTracks.PLAYLIST_ID));
        }
    }

    private static class LikedTrackMapper implements ResultMapper<OfflineRequestData> {
        @Override
        public OfflineRequestData map(CursorReader reader) {
            return new OfflineRequestData(
                    reader.getLong(_ID),
                    reader.getLong(TableColumns.Sounds.DURATION),
                    true);
        }
    }

}
