package com.soundcloud.android.offline;

import static android.provider.BaseColumns._ID;
import static com.soundcloud.android.offline.IsOfflineLikedTracksEnabledCommand.isOfflineLikesEnabledQuery;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.Table.TrackPolicies;
import static com.soundcloud.android.storage.TableColumns.Sounds.ARTWORK_URL;
import static com.soundcloud.android.storage.TableColumns.Sounds.FULL_DURATION;
import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_TRACK;
import static com.soundcloud.android.storage.TableColumns.Sounds.USER_ID;
import static com.soundcloud.android.storage.TableColumns.Sounds.WAVEFORM_URL;
import static com.soundcloud.android.storage.TableColumns.Sounds._TYPE;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.LAST_UPDATED;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.SNIPPED;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.SYNCABLE;
import static com.soundcloud.android.storage.Tables.OfflineContent;
import static com.soundcloud.java.collections.MoreCollections.transform;
import static com.soundcloud.propeller.query.Filter.filter;
import static com.soundcloud.propeller.query.Query.Order.ASC;
import static com.soundcloud.propeller.query.Query.Order.DESC;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.OfflinePlaylistTracks;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.CursorReader;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.ResultMapper;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;

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
            .whereEq(Likes.field(TableColumns.Likes._ID), Sounds.field(_ID))
            .whereEq(Likes.field(_TYPE), TableColumns.Sounds.TYPE_TRACK);

    private static final Function<OfflineRequestData, Urn> TO_URN = new Function<OfflineRequestData, Urn>() {
        @Override
        public Urn apply(OfflineRequestData input) {
            return input.track;
        }
    };

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
        final Query likesToDownload = Query.from(Sounds.name())
                                           .select(
                                                   Sounds.field(_ID),
                                                   Sounds.field(FULL_DURATION),
                                                   Sounds.field(WAVEFORM_URL),
                                                   Sounds.field(ARTWORK_URL),
                                                   Sounds.field(USER_ID),
                                                   TrackPolicies.field(SYNCABLE),
                                                   TrackPolicies.field(SNIPPED))
                                           .innerJoin(Likes.name(), LIKES_SOUNDS_FILTER)
                                           .innerJoin(TrackPolicies.name(),
                                                      Likes.field(TableColumns.Likes._ID),
                                                      TableColumns.TrackPolicies.TRACK_ID)
                                           .where(isDownloadable(TrackPolicies.field(LAST_UPDATED)))
                                           .whereEq(Sounds.field(_TYPE), TYPE_TRACK)
                                           .whereNull(Likes.field(TableColumns.Likes.REMOVED_AT))
                                           .order(Likes.field(TableColumns.Likes.CREATED_AT), DESC);

        return database.query(likesToDownload).toList(new LikedTrackMapper());
    }

    private boolean isOfflineLikedTracksEnabled() {
        return database
                .query(isOfflineLikesEnabledQuery())
                .first(Boolean.class);
    }

    private Where isDownloadable(String policyUpdatedColumnName) {
        long lastUpdatedThreshold = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        return filter().whereGt(policyUpdatedColumnName, lastUpdatedThreshold);
    }

    private List<OfflineRequestData> tracksFromOfflinePlaylists() {
        return database.query(Query.from(OfflinePlaylistTracks.TABLE)
                                   .where(isDownloadable(OfflinePlaylistTracks.LAST_POLICY_UPDATE.name()))
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
                    reader.getLong(_ID),
                    Optional.fromNullable(reader.getString(ARTWORK_URL)),
                    reader.getLong(USER_ID),
                    reader.getLong(FULL_DURATION),
                    reader.getString(WAVEFORM_URL),
                    reader.getBoolean(SYNCABLE),
                    reader.getBoolean(SNIPPED));
        }
    }
}
