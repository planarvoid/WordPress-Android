package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.IsOfflineLikedTracksEnabledCommand.isOfflineLikesEnabledQuery;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Tables.OfflineContent;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_TRACK;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.ASC;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.OfflinePlaylistTracks;
import com.soundcloud.android.storage.Tables.Sounds;
import com.soundcloud.android.storage.Tables.TrackPolicies;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import com.soundcloud.propeller.schema.Column;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

class LoadExpectedContentCommand extends Command<Object, ExpectedOfflineContent> {
    private final static Where LIKES_SOUNDS_FILTER = filter()
            .whereEq(Tables.Likes._ID, Sounds._ID)
            .whereEq(Tables.Likes._TYPE, Sounds.TYPE_TRACK);

    private static final Function<OfflineRequestData, Urn> TO_URN = input -> input.track;

    private final PropellerDatabase database;

    @Inject
    LoadExpectedContentCommand(PropellerDatabase database) {
        this.database = database;
    }

    @Override
    public ExpectedOfflineContent call(Object ignored) {
        final List<OfflineRequestData> likedTracks = tracksFromLikes();
        final Collection<DownloadRequest> downloadRequests =
                getAggregatedRequestData(likedTracks, tracksFromOfflinePlaylists());

        return new ExpectedOfflineContent(
                downloadRequests,
                getPlaylistsWithoutTracks(),
                isOfflineLikedTracksEnabled(),
                transform(likedTracks, TO_URN)
        );
    }

    private Collection<Urn> getPlaylistsWithoutTracks() {
        return database.query(Query
                                      .from(OfflineContent.TABLE)
                                      .leftJoin(PlaylistTracks,
                                                filter().whereEq(OfflineContent._ID,
                                                                 TableColumns.PlaylistTracks.PLAYLIST_ID))
                                      .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)
                                      .whereNull(PlaylistTracks.field(TableColumns.PlaylistTracks.PLAYLIST_ID)))
                       .toList(new PlaylistUrnMapper());
    }

    private List<OfflineRequestData> tracksFromLikes() {
        if (isOfflineLikedTracksEnabled()) {
            return requestTracksFromLikes();
        }
        return Collections.emptyList();
    }

    private Collection<DownloadRequest> getAggregatedRequestData(List<OfflineRequestData> likesRequests,
                                                                 List<OfflineRequestData> playlistTracks) {
        final HashMap<Urn, DownloadRequest> requestsMap = new LinkedHashMap<>(likesRequests.size() + playlistTracks.size());

        final List<OfflineRequestData> allRequests = new ArrayList<>(likesRequests.size() + playlistTracks.size());
        allRequests.addAll(playlistTracks);
        allRequests.addAll(likesRequests);

        for (OfflineRequestData data : allRequests) {
            if (!requestsMap.containsKey(data.track)) {
                requestsMap.put(data.track,
                                DownloadRequest.create(data.track,
                                                       data.getImageUrlTemplate(),
                                                       data.duration,
                                                       data.waveformUrl,
                                                       data.syncable,
                                                       data.snipped,
                                                       data.trackingMetadata));
            } else {
                requestsMap.get(data.track).getTrackingData().update(data.trackingMetadata);
            }
        }
        return requestsMap.values();
    }

    private List<OfflineRequestData> requestTracksFromLikes() {
        final Query likesToDownload = Query.from(Sounds.TABLE)
                                           .select(
                                                   Sounds._ID,
                                                   Sounds.FULL_DURATION,
                                                   Sounds.WAVEFORM_URL,
                                                   Sounds.ARTWORK_URL,
                                                   Sounds.USER_ID,
                                                   TrackPolicies.SYNCABLE,
                                                   TrackPolicies.SNIPPED)
                                           .innerJoin(Tables.Likes.TABLE, LIKES_SOUNDS_FILTER)
                                           .innerJoin(TrackPolicies.TABLE,
                                                      Tables.Likes._ID,
                                                      TrackPolicies.TRACK_ID)
                                           .where(isDownloadable(TrackPolicies.LAST_UPDATED))
                                           .whereEq(Sounds._TYPE, TYPE_TRACK)
                                           .whereNull(Tables.Likes.REMOVED_AT)
                                           .order(Tables.Likes.CREATED_AT, DESC);

        return database.query(likesToDownload).toList(new LikedTrackMapper());
    }

    private boolean isOfflineLikedTracksEnabled() {
        return database
                .query(isOfflineLikesEnabledQuery())
                .first(Boolean.class);
    }

    private Where isDownloadable(Column policyUpdatedColumnName) {
        long lastUpdatedThreshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        return filter().whereGt(policyUpdatedColumnName, lastUpdatedThreshold);
    }

    private List<OfflineRequestData> tracksFromOfflinePlaylists() {
        return database.query(Query.from(OfflinePlaylistTracks.TABLE)
                                   .where(isDownloadable(OfflinePlaylistTracks.LAST_POLICY_UPDATE))
                                   .order(OfflinePlaylistTracks.CREATED_AT, DESC)
                                   .order(OfflinePlaylistTracks.POSITION, ASC)
        ).toList(new OfflinePlaylistTrackMapper());
    }

    private static class OfflineRequestData implements ImageResource {
        final Urn track;
        final long duration;
        final String waveformUrl;
        final boolean syncable;
        final boolean snipped;
        final TrackingMetadata trackingMetadata;
        private final Optional<String> imageUrlTemplate;

        @Override
        public Urn getUrn() {
            return track;
        }

        @Override
        public Optional<String> getImageUrlTemplate() {
            return imageUrlTemplate;
        }

        static OfflineRequestData fromLikes(long trackId,
                                            Optional<String> imageUrlTemplate,
                                            long creatorId,
                                            long duration,
                                            String waveformUrl,
                                            boolean syncable,
                                            boolean snipped) {
            return new OfflineRequestData(trackId, imageUrlTemplate, creatorId, duration,
                                          waveformUrl, syncable, snipped, true, false);
        }

        static OfflineRequestData fromPlaylist(long trackId,
                                               Optional<String> imageUrlTemplate,
                                               long creatorId,
                                               long duration,
                                               String waveformUrl,
                                               boolean syncable,
                                               boolean snipped) {
            return new OfflineRequestData(trackId, imageUrlTemplate, creatorId, duration,
                                          waveformUrl, syncable, snipped, false, true);
        }

        private OfflineRequestData(long trackId,
                                   Optional<String> imageUrlTemplate,
                                   long creatorId,
                                   long duration,
                                   String waveformUrl,
                                   boolean syncable,
                                   boolean snipped,
                                   boolean fromLikes,
                                   boolean fromPlaylists) {
            this.imageUrlTemplate = imageUrlTemplate;
            this.track = Urn.forTrack(trackId);
            this.duration = duration;
            this.waveformUrl = waveformUrl;
            this.syncable = syncable;
            this.snipped = snipped;
            this.trackingMetadata = new TrackingMetadata(Urn.forUser(creatorId), fromLikes, fromPlaylists);
        }
    }

    private static class OfflinePlaylistTrackMapper implements ResultMapper<OfflineRequestData> {

        @Override
        public OfflineRequestData map(CursorReader reader) {
            return OfflineRequestData.fromPlaylist(
                    reader.getLong(OfflinePlaylistTracks._ID),
                    Optional.fromNullable(reader.getString(OfflinePlaylistTracks.ARTWORK_URL)),
                    reader.getLong(OfflinePlaylistTracks.USER_ID),
                    reader.getLong(OfflinePlaylistTracks.DURATION),
                    reader.getString(OfflinePlaylistTracks.WAVEFORM_URL),
                    reader.getBoolean(OfflinePlaylistTracks.SYNCABLE),
                    reader.getBoolean(OfflinePlaylistTracks.SNIPPED));
        }
    }

    private static class LikedTrackMapper implements ResultMapper<OfflineRequestData> {

        @Override
        public OfflineRequestData map(CursorReader reader) {
            return OfflineRequestData.fromLikes(
                    reader.getLong(Sounds._ID),
                    Optional.fromNullable(reader.getString(Sounds.ARTWORK_URL)),
                    reader.getLong(Sounds.USER_ID),
                    reader.getLong(Sounds.FULL_DURATION),
                    reader.getString(Sounds.WAVEFORM_URL),
                    reader.getBoolean(TrackPolicies.SYNCABLE),
                    reader.getBoolean(TrackPolicies.SNIPPED));
        }
    }
}
