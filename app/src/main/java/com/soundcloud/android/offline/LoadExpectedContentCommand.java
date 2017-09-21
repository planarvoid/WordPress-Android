package com.soundcloud.android.offline;

import static com.soundcloud.android.offline.IsOfflineLikedTracksEnabledCommand.isOfflineLikesEnabledQuery;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Tables.OfflineContent;
import static com.soundcloud.android.utils.RepoUtilsKt.enrichItemsWithProperties;
import static com.soundcloud.propeller.query.Filter.filter;

import com.soundcloud.android.commands.Command;
import com.soundcloud.android.commands.PlaylistUrnMapper;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.likes.LikesStorage;
import com.soundcloud.android.model.Association;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UrnHolder;
import com.soundcloud.android.playlists.LoadPlaylistTracksCommand;
import com.soundcloud.android.playlists.Playlist;
import com.soundcloud.android.playlists.PlaylistStorage;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackStorage;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.Maps;
import com.soundcloud.java.collections.MoreCollections;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.query.Query;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.annotations.NonNull;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class LoadExpectedContentCommand extends Command<Object, ExpectedOfflineContent> {

    private static final long STALE_POLICY_CUTOFF_TIME = TimeUnit.DAYS.toMillis(30);

    private final PropellerDatabase database;
    private final LikesStorage likesStorage;
    private final TrackStorage trackStorage;
    private final LoadPlaylistTracksCommand loadPlaylistTracksCommand;
    private final LoadOfflinePlaylistsCommand playlistsCommand;
    private final PlaylistStorage playlistStorage;

    @Inject
    LoadExpectedContentCommand(PropellerDatabase database,
                               LikesStorage likesStorage,
                               TrackStorage trackStorage,
                               LoadPlaylistTracksCommand loadPlaylistTracksCommand,
                               LoadOfflinePlaylistsCommand playlistsCommand,
                               PlaylistStorage playlistStorage) {
        this.database = database;
        this.likesStorage = likesStorage;
        this.trackStorage = trackStorage;
        this.loadPlaylistTracksCommand = loadPlaylistTracksCommand;
        this.playlistsCommand = playlistsCommand;
        this.playlistStorage = playlistStorage;
    }

    @Override
    public ExpectedOfflineContent call(Object ignored) {
        final List<OfflineRequestData> likedTracks = tracksFromLikes();
        final Collection<DownloadRequest> downloadRequests = getAggregatedRequestData(likedTracks, tracksFromOfflinePlaylists());

        List<Urn> transform = Lists.transform(likedTracks, input -> input.track);
        return new ExpectedOfflineContent(
                downloadRequests,
                getPlaylistsWithoutTracks(),
                isOfflineLikedTracksEnabled(),
                transform
        );
    }

    private Collection<Urn> getPlaylistsWithoutTracks() {
        return database.query(Query.from(OfflineContent.TABLE)
                                   .leftJoin(PlaylistTracks, filter().whereEq(OfflineContent._ID, TableColumns.PlaylistTracks.PLAYLIST_ID))
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
            if (requestsMap.containsKey(data.track)) {
                requestsMap.get(data.track).getTrackingData().update(data.trackingMetadata);
            } else {
                requestsMap.put(data.track,
                                DownloadRequest.create(data.track,
                                                       data.getImageUrlTemplate(),
                                                       data.duration,
                                                       data.waveformUrl,
                                                       data.syncable,
                                                       data.snipped,
                                                       data.trackingMetadata));
            }
        }
        return requestsMap.values();
    }

    private List<OfflineRequestData> requestTracksFromLikes() {
        return likesStorage.loadTrackLikes()
                           .flatMap(source -> enrichItemsWithProperties(source, trackStorage.loadTracks(Lists.transform(source, UrnHolder::urn)), (track, like) -> track))
                           .map(this::filterTracksForStalePolicies)
                           .map(tracks -> Lists.transform(Lists.newArrayList(tracks), input -> OfflineRequestData.fromLikes(
                              input.urn(),
                              input.imageUrlTemplate(),
                              input.creatorUrn(),
                              input.fullDuration(),
                              input.waveformUrl(),
                              input.isSyncable(),
                              input.snipped()

                      ))).blockingGet();
    }

    private Collection<Track> filterTracksForStalePolicies(List<Track> tracks) {
        return MoreCollections.filter(tracks, input -> input.policyLastUpdatedAt().getTime() > stalePolicyCutoff());
    }

    private boolean isOfflineLikedTracksEnabled() {
        return database
                .query(isOfflineLikesEnabledQuery())
                .first(Boolean.class);
    }

    private long stalePolicyCutoff() {
        return System.currentTimeMillis() - STALE_POLICY_CUTOFF_TIME;
    }

    private List<OfflineRequestData> tracksFromOfflinePlaylists() {
        Single<List<Association>> playlistLikes = likesStorage.loadPlaylistLikes();
        Single<List<Urn>> offlinePlaylists = playlistsCommand.toSingle();
        return offlinePlaylists.flatMap(playlistStorage::loadPlaylists)
                               .zipWith(playlistLikes, this::orderedOfflinePlaylists)
                               .flatMap((playlists) -> Observable.fromIterable(playlists)
                                                                 .concatMap(this::playlistTracksForDownoad)
                                                                 .collect(ArrayList::new, this::toPlaylistTrackOfflineRequestData))
                               .blockingGet();
    }

    private void toPlaylistTrackOfflineRequestData(List<OfflineRequestData> offlineRequestDatas, Collection<Track> tracks) {
        for (Track track : tracks) {
            offlineRequestDatas.add(OfflineRequestData.fromPlaylist(
                    track.urn(),
                    track.imageUrlTemplate(),
                    track.creatorUrn(),
                    track.fullDuration(),
                    track.waveformUrl(),
                    track.isSyncable(),
                    track.snipped()
            ));
        }
    }

    private Observable<Collection<Track>> playlistTracksForDownoad(@NonNull Playlist playlist) {
        return loadPlaylistTracksCommand.toSingle(playlist.urn())
                                        .map(this::filterTracksForStalePolicies)
                                        .toObservable();
    }

    @android.support.annotation.NonNull
    private List<Playlist> orderedOfflinePlaylists(List<Playlist> playlists, List<Association> associations) {
        Map<Urn, Association> likesMap = Maps.asMap(associations, Association::urn);
        ArrayList<Playlist> orderedPlaylists = new ArrayList<>(playlists);
        Collections.sort(orderedPlaylists, (t, t1) -> getLikedOrPostedAtDate(t1, likesMap).compareTo(getLikedOrPostedAtDate(t, likesMap)));
        return orderedPlaylists;
    }

    private Date getLikedOrPostedAtDate(Playlist t, Map<Urn, Association> likesMap) {
        return likesMap.containsKey(t.urn()) ? likesMap.get(t.urn()).getCreatedAt() : t.createdAt();
    }

    private final static class OfflineRequestData implements ImageResource {

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

        static OfflineRequestData fromLikes(Urn trackUrn,
                                            Optional<String> imageUrlTemplate,
                                            Urn creatorUrn,
                                            long duration,
                                            String waveformUrl,
                                            boolean syncable,
                                            boolean snipped) {
            return new OfflineRequestData(trackUrn, imageUrlTemplate, creatorUrn, duration,
                                          waveformUrl, syncable, snipped, true, false);
        }

        static OfflineRequestData fromPlaylist(Urn trackUrn,
                                               Optional<String> imageUrlTemplate,
                                               Urn creatorUrn,
                                               long duration,
                                               String waveformUrl,
                                               boolean syncable,
                                               boolean snipped) {
            return new OfflineRequestData(trackUrn, imageUrlTemplate, creatorUrn, duration,
                                          waveformUrl, syncable, snipped, false, true);
        }

        private OfflineRequestData(Urn trackUrn,
                                   Optional<String> imageUrlTemplate,
                                   Urn creatorUrn,
                                   long duration,
                                   String waveformUrl,
                                   boolean syncable,
                                   boolean snipped,
                                   boolean fromLikes,
                                   boolean fromPlaylists) {
            this.imageUrlTemplate = imageUrlTemplate;
            this.track = trackUrn;
            this.duration = duration;
            this.waveformUrl = waveformUrl;
            this.syncable = syncable;
            this.snipped = snipped;
            this.trackingMetadata = new TrackingMetadata(creatorUrn, fromLikes, fromPlaylists);
        }
    }
}
