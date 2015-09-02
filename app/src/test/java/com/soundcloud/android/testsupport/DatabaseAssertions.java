package com.soundcloud.android.testsupport;

import static com.soundcloud.java.collections.Lists.transform;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.StationRecord;
import com.soundcloud.android.api.model.stream.ApiPromotedPlaylist;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.RecentStations;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.waveform.WaveformSerializer;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.test.matchers.QueryBinding;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.Date;
import java.util.List;

public class DatabaseAssertions {

    private final Function<TrackRecord, String> toUrn = new Function<TrackRecord, String>() {
        @Override
        public String apply(TrackRecord track) {
            return track.getUrn().toString();
        }
    };
    private SQLiteDatabase database;

    public DatabaseAssertions(SQLiteDatabase database) {
        this.database = database;
    }

    public void assertTrackWithUserInserted(ApiTrack track) {
        assertTrackInserted(track);
        assertPlayableUserInserted(track.getUser());
    }

    public void assertPlaylistWithUserInserted(ApiPlaylist playlist) {
        assertPlaylistInserted(playlist);
        assertPlayableUserInserted(playlist.getUser());
    }

    public void assertTrackInserted(ApiTrack track) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, track.getId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(TableColumns.Sounds.TITLE, track.getTitle())
                .whereEq(TableColumns.Sounds.DURATION, track.getDuration())
                .whereEq(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl())
                .whereEq(TableColumns.Sounds.STREAM_URL, track.getStreamUrl())
                .whereEq(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl())
                .whereEq(TableColumns.Sounds.CREATED_AT, track.getCreatedAt().getTime())
                .whereEq(TableColumns.Sounds.GENRE, track.getGenre())
                .whereEq(TableColumns.Sounds.SHARING, track.getSharing().value())
                .whereEq(TableColumns.Sounds.USER_ID, track.getUser().getUrn().getNumericId())
                .whereEq(TableColumns.Sounds.COMMENTABLE, track.isCommentable())
                .whereEq(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount())
                .whereEq(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount())
                .whereEq(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount())
                .whereEq(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount())), counts(1));

        assertTrackPolicyInserted(track);
    }

    public void assertTrackIsUnavailable(Urn trackUrn, long time) {
        assertThat(select(from(TrackDownloads.TABLE)
                .whereEq(TrackDownloads._ID, trackUrn.getNumericId())
                .whereEq(TrackDownloads.UNAVAILABLE_AT, time)), counts(1));
    }

    public void assertDownloadIsAvailable(Urn track) {
        assertThat(select(from(TrackDownloads.TABLE)
                .whereEq(TrackDownloads._ID, track.getNumericId())
                .whereNull(TrackDownloads.UNAVAILABLE_AT)), counts(1));
    }

    public void assertPlaylistTrackForRemoval(long playlistId, Urn urn) {
        assertThat(select(from(Table.PlaylistTracks.name())
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistId)
                .whereEq(TableColumns.PlaylistTracks.TRACK_ID, urn.getNumericId())
                .whereNotNull(TableColumns.PlaylistTracks.REMOVED_AT)), counts(1));
    }

    public void assertUserFollowingsPending(Urn targetUrn, boolean following) {
        Query query = from(Table.UserAssociations.name())
                .whereEq(TableColumns.UserAssociations.TARGET_ID, targetUrn.getNumericId())
                .whereEq(TableColumns.UserAssociations.ASSOCIATION_TYPE, TableColumns.UserAssociations.TYPE_FOLLOWING)
                .whereEq(TableColumns.UserAssociations.RESOURCE_TYPE, TableColumns.UserAssociations.TYPE_RESOURCE_USER)
                .whereNotNull(TableColumns.UserAssociations.CREATED_AT);

        if (following) {
            query.whereNotNull(TableColumns.UserAssociations.ADDED_AT)
                    .whereNull(TableColumns.UserAssociations.REMOVED_AT);
        } else {
            query.whereNull(TableColumns.UserAssociations.ADDED_AT)
                    .whereNotNull(TableColumns.UserAssociations.REMOVED_AT);
        }

        assertThat(select(query), counts(1));
    }

    public void assertUserFollowersCount(Urn targetUrn, int numberOfFollowers) {
        assertThat(select(from(Table.Users.name())
                .whereEq(TableColumns.Users._ID, targetUrn.getNumericId())
                .whereEq(TableColumns.Users.FOLLOWERS_COUNT, numberOfFollowers)), counts(1));
    }

    public void assertStationInserted(StationRecord station) {
        assertThat(
                select(
                        from(Stations.TABLE)
                                .whereEq(Stations.STATION_URN, station.getUrn())
                                .whereEq(Stations.TITLE, station.getTitle())
                                .whereEq(Stations.TYPE, station.getType())
                                .whereEq(Stations.PERMALINK, station.getPermalink())
                                .whereEq(Stations.LAST_PLAYED_TRACK_POSITION, 0)
                ),
                counts(1)
        );
        assertThat(
                select(from(StationsPlayQueues.TABLE)
                                .whereEq(StationsPlayQueues.STATION_URN, station.getUrn().toString())
                                .whereIn(StationsPlayQueues.TRACK_URN, transform(station.getTracks().getCollection(), toUrn))
                ),
                counts(station.getTracks().getCollection().size())
        );
    }

    public void assertStationIsUnique(Urn station) {
        assertThat(
                select(from(Stations.TABLE).whereEq(Stations.STATION_URN, station)),
                counts(1)
        );
    }

    public void assertStationPosition(Urn station, int position) {
        assertThat(
                select(
                        from(Stations.TABLE)
                                .whereEq(Stations.STATION_URN, station.toString())
                                .whereEq(Stations.LAST_PLAYED_TRACK_POSITION, position)
                ),
                counts(1)
        );
    }

    public void assertStationsPlayQueueIsEmpty(StationRecord station) {
        assertThat(
                select(from(StationsPlayQueues.TABLE)
                        .whereEq(StationsPlayQueues.STATION_URN, station.getUrn().toString())),
                counts(0)
        );
    }

    public void assertRecentStationsContains(Urn stationUrn, long currentTime, int expectedCount) {
        assertThat(
                select(from(RecentStations.TABLE)
                        .whereEq(RecentStations.STATION_URN, stationUrn.toString())
                        .whereEq(RecentStations.UPDATED_LOCALLY_AT, currentTime)),
                counts(expectedCount)
        );
    }

    private void assertTrackPolicyInserted(ApiTrack track) {
        final Query query = from(Table.TrackPolicies.name())
                .whereEq(TableColumns.TrackPolicies.TRACK_ID, track.getId())
                .whereEq(TableColumns.TrackPolicies.MONETIZABLE, track.isMonetizable())
                .whereEq(TableColumns.TrackPolicies.SYNCABLE, track.isSyncable())
                .whereEq(TableColumns.TrackPolicies.POLICY, track.getPolicy());

        if (track.getMonetizationModel().isPresent()){
            query.whereEq(TableColumns.TrackPolicies.MONETIZATION_MODEL, track.getMonetizationModel().get());
        }
        if (track.isSubMidTier().isPresent()){
            query.whereEq(TableColumns.TrackPolicies.SUB_MID_TIER, track.isSubMidTier().get());
        }
        if (track.isSubHighTier().isPresent()){
            query.whereEq(TableColumns.TrackPolicies.SUB_HIGH_TIER, track.isSubHighTier().get());
        }
        assertThat(select(query), counts(1));
    }

    public void assetPolicyInserted(Urn trackUrn, boolean monetizable, String policy, boolean syncable) {
        assertThat(select(from(Table.SoundView.name())
                        .whereEq(TableColumns.SoundView._ID, trackUrn.getNumericId())
                        .whereEq(TableColumns.SoundView._TYPE, TableColumns.Sounds.TYPE_TRACK)
                        .whereEq(TableColumns.SoundView.POLICIES_MONETIZABLE, monetizable)
                        .whereEq(TableColumns.SoundView.POLICIES_POLICY, policy)
                        .whereEq(TableColumns.SoundView.POLICIES_SYNCABLE, syncable)
        ), counts(1));
    }

    public void assertLikedTrackPendingAddition(Urn targetUrn) {
        assertLikedPendingAddition(targetUrn, TableColumns.Sounds.TYPE_TRACK);
    }

    public void assertLikedPlaylistPendingAddition(Urn targetUrn) {
        assertLikedPendingAddition(targetUrn, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    public void assertLikesCount(Urn urn, int newLikesCount) {
        assertThat(select(from(Table.SoundView.name())
                .whereEq(TableColumns.SoundView._ID, urn.getNumericId())
                .whereEq(TableColumns.SoundView._TYPE, urn.isTrack()
                        ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.SoundView.LIKES_COUNT, newLikesCount)), counts(1));
    }

    private void assertLikedPendingAddition(Urn targetUrn, int type) {
        assertThat(select(from(Table.Likes.name())
                .whereEq(TableColumns.Likes._ID, targetUrn.getNumericId())
                .whereEq(TableColumns.Likes._TYPE, type)
                .whereNotNull(TableColumns.Likes.ADDED_AT)
                .whereNull(TableColumns.Likes.REMOVED_AT)), counts(1));
    }

    public void assertLikedTrackPendingRemoval(Urn targetUrn) {
        assertLikedPendingRemoval(targetUrn, TableColumns.Sounds.TYPE_TRACK);
    }

    public void assertLikedPlaylistPendingRemoval(Urn targetUrn) {
        assertLikedPendingRemoval(targetUrn, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private void assertLikedPendingRemoval(Urn targetUrn, int type) {
        assertThat(select(from(Table.Likes.name())
                .whereEq(TableColumns.Likes._ID, targetUrn.getNumericId())
                .whereEq(TableColumns.Likes._TYPE, type)
                .whereNull(TableColumns.Likes.ADDED_AT)
                .whereNotNull(TableColumns.Likes.REMOVED_AT)), counts(1));
    }

    public void assertPlayableUserInserted(UserRecord user) {
        assertThat(select(from(Table.SoundView.name())
                        .whereEq(TableColumns.SoundView.USER_ID, user.getUrn().getNumericId())
                        .whereEq(TableColumns.SoundView.USERNAME, user.getUsername())
        ), counts(1));
    }


    public void assertPlaylistNotStored(ApiPlaylist playlist) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlist.getId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)), counts(0));
    }

    public void assertPlaylistInserted(long playlistId) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlistId)
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)), counts(1));
    }

    public void assertPlaylistInserted(ApiPlaylist playlist) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlist.getId())
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Sounds.TITLE, playlist.getTitle())
                .whereEq(TableColumns.Sounds.DURATION, playlist.getDuration())
                .whereEq(TableColumns.Sounds.CREATED_AT, playlist.getCreatedAt().getTime())
                .whereEq(TableColumns.Sounds.PERMALINK_URL, playlist.getPermalinkUrl())
                .whereEq(TableColumns.Sounds.SHARING, playlist.getSharing().value())
                .whereEq(TableColumns.Sounds.USER_ID, playlist.getUser().getId())
                .whereEq(TableColumns.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount())
                .whereEq(TableColumns.Sounds.REPOSTS_COUNT, playlist.getStats().getRepostsCount())
                .whereEq(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount())
                .whereEq(TableColumns.Sounds.TAG_LIST, TextUtils.join(" ", playlist.getTags()))), counts(1));
    }

    public void assertPlaylistInserted(long playlistId, String title, boolean isPrivate) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlistId)
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Sounds.USER_ID, 321L)
                .whereNotNull(TableColumns.Sounds.CREATED_AT)
                .whereEq(TableColumns.Sounds.SHARING, Sharing.from(!isPrivate).value())
                .whereEq(TableColumns.Sounds.TITLE, title)), counts(1));
    }

    public void assertPlaylistTracklist(long playlistId, List<Urn> tracklist) {
        for (int i = 0; i < tracklist.size(); i++) {
            assertThat(select(from(Table.PlaylistTracks.name())
                    .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistId)
                    .whereEq(TableColumns.PlaylistTracks.TRACK_ID, tracklist.get(i).getNumericId())
                    .whereEq(TableColumns.PlaylistTracks.POSITION, i)), counts(1));
        }

        // assert no additional tracks
        assertThat(select(from(Table.PlaylistTracks.name())
                .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistId)
                .whereNull(TableColumns.PlaylistTracks.REMOVED_AT)
                .whereGe(TableColumns.PlaylistTracks.POSITION, tracklist.size())), counts(0));
    }

    public void assertPlaylistPostInsertedFor(ApiPlaylist playlist) {
        assertPlaylistPostInsertedFor(playlist.getUrn());
    }

    public void assertPlaylistPostInsertedFor(Urn playlistUrn) {
        assertThat(select(from(Table.Posts.name())
                .whereEq(TableColumns.Posts.TARGET_ID, playlistUrn.getNumericId())
                .whereEq(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)), counts(1));
    }

    public void assertTrackRepostInserted(Urn urn, Date createdAt) {
        assertThat(select(from(Table.Posts.name())
                .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                .whereEq(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(TableColumns.Posts.CREATED_AT, createdAt.getTime())
                .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST)), counts(1));

    }

    public void assertTrackRepostNotExistent(Urn urn) {
        assertThat(select(from(Table.Posts.name())
                .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                .whereEq(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_TRACK)
                .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST)), counts(0));
    }

    public void assertPlaylistRepostInserted(Urn urn, Date createdAt) {
        assertThat(select(from(Table.Posts.name())
                .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                .whereEq(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Posts.CREATED_AT, createdAt.getTime())
                .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST)), counts(1));
    }

    public void assertPlaylistRepostNotExistent(Urn urn) {
        assertThat(select(from(Table.Posts.name())
                .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                .whereEq(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Posts.TYPE, TableColumns.Posts.TYPE_REPOST)), counts(0));
    }

    public void assertPromotionInserted(ApiPromotedTrack promotedTrack) {
        assertThat(select(attachPromotedTrackingQueries(
                        from(Table.PromotedTracks.name())
                                .whereEq(TableColumns.PromotedTracks.AD_URN, promotedTrack.getAdUrn())
                                .whereEq(TableColumns.PromotedTracks.PROMOTER_ID, promotedTrack.getPromoter().getId()),
                        promotedTrack)
        ), counts(1));
    }

    public void assertPromotionInserted(ApiPromotedPlaylist promotedPlaylist) {
        assertThat(select(attachPromotedTrackingQueries(
                        from(Table.PromotedTracks.name())
                                .whereEq(TableColumns.PromotedTracks.AD_URN, promotedPlaylist.getAdUrn())
                                .whereEq(TableColumns.PromotedTracks.PROMOTER_ID, promotedPlaylist.getPromoter().getId()),
                        promotedPlaylist)
        ), counts(1));
    }

    public void assertPromotionWithoutPromoterInserted(ApiPromotedTrack promotedTrack) {
        assertThat(select(attachPromotedTrackingQueries(
                        from(Table.PromotedTracks.name())
                                .whereEq(TableColumns.PromotedTracks.AD_URN, promotedTrack.getAdUrn())
                                .whereNull(TableColumns.PromotedTracks.PROMOTER_ID),
                        promotedTrack)
        ), counts(1));
    }

    public void assertPromotionWithoutPromoterInserted(ApiPromotedPlaylist promotedPlaylist) {
        assertThat(select(attachPromotedTrackingQueries(
                        from(Table.PromotedTracks.name())
                                .whereEq(TableColumns.PromotedTracks.AD_URN, promotedPlaylist.getAdUrn())
                                .whereNull(TableColumns.PromotedTracks.PROMOTER_ID),
                        promotedPlaylist)
        ), counts(1));
    }

    private Query attachPromotedTrackingQueries(Query query, ApiPromotedTrack promotedTrack) {
        return query.whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS, TextUtils.join(" ", promotedTrack.getTrackingTrackPlayedUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS, TextUtils.join(" ", promotedTrack.getTrackingTrackClickedUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS, TextUtils.join(" ", promotedTrack.getTrackingTrackImpressionUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_PROFILE_CLICKED_URLS, TextUtils.join(" ", promotedTrack.getTrackingProfileClickedUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS, TextUtils.join(" ", promotedTrack.getTrackingPromoterClickedUrls()));
    }

    private Query attachPromotedTrackingQueries(Query query, ApiPromotedPlaylist promotedPlaylist) {
        return query.whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS, TextUtils.join(" ", promotedPlaylist.getTrackingTrackPlayedUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS, TextUtils.join(" ", promotedPlaylist.getTrackingPlaylistClickedUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS, TextUtils.join(" ", promotedPlaylist.getTrackingPlaylistImpressionUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_PROFILE_CLICKED_URLS, TextUtils.join(" ", promotedPlaylist.getTrackingProfileClickedUrls()))
                .whereEq(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS, TextUtils.join(" ", promotedPlaylist.getTrackingPromoterClickedUrls()));
    }

    public void assertUserInserted(UserRecord user) {
        final Query query = from(Table.Users.name())
                .whereEq(TableColumns.Users._ID, user.getUrn().getNumericId())
                .whereEq(TableColumns.Users.USERNAME, user.getUsername())
                .whereEq(TableColumns.Users.COUNTRY, user.getCountry())
                .whereEq(TableColumns.Users.FOLLOWERS_COUNT, user.getFollowersCount());

        assertOptionalColumn(query, TableColumns.Users.DESCRIPTION, user.getDescription());
        assertOptionalColumn(query, TableColumns.Users.WEBSITE_URL, user.getWebsiteUrl());
        assertOptionalColumn(query, TableColumns.Users.WEBSITE_NAME, user.getWebsiteName());
        assertOptionalColumn(query, TableColumns.Users.DISCOGS_NAME, user.getDiscogsName());
        assertOptionalColumn(query, TableColumns.Users.MYSPACE_NAME, user.getMyspaceName());
        assertThat(select(query), counts(1));
    }

    private void assertOptionalColumn(Query query, String column, Optional<String> optional) {
        if (optional.isPresent()) {
            query.whereEq(column, optional.get());
        } else {
            query.whereNull(column);
        }
    }

    public void assertDownloadRequestsInserted(List<Urn> tracksToDownload) {
        for (Urn urn : tracksToDownload) {
            assertThat(select(from(TrackDownloads.TABLE)
                    .whereEq(TrackDownloads._ID, urn.getNumericId())), counts(1));
        }
    }

    public void assertDownloadPendingRemoval(Urn trackUrn) {
        assertThat(select(from(TrackDownloads.TABLE)
                .whereEq(TrackDownloads._ID, trackUrn.getNumericId())
                .whereNotNull(TrackDownloads.DOWNLOADED_AT)
                .whereNotNull(TrackDownloads.REMOVED_AT)), counts(1));
    }

    public void assertDownloadResultsInserted(DownloadState result) {
        assertThat(select(from(TrackDownloads.TABLE)
                .whereNull(TrackDownloads.UNAVAILABLE_AT)
                .whereEq(TrackDownloads._ID, result.getTrack().getNumericId())
                .whereEq(TrackDownloads.DOWNLOADED_AT, result.getTimestamp())), counts(1));
    }

    public void assertNotDownloaded(Urn trackUrn) {
        assertThat(select(from(TrackDownloads.TABLE)
                .whereEq(TrackDownloads._ID, trackUrn.getNumericId())), counts(0));
    }

    public void assertDownloadedAndNotMarkedForRemoval(Urn trackUrn) {
        assertThat(select(from(TrackDownloads.TABLE)
                .whereEq(TrackDownloads._ID, trackUrn.getNumericId())
                .whereNotNull(TrackDownloads.DOWNLOADED_AT)
                .whereNull(TrackDownloads.REMOVED_AT)), counts(1));
    }

    protected QueryBinding select(Query query) {
        return new QueryBinding(this.database, query);
    }

    public void assertPlaylistMarkedForOfflineSync(Urn playlistUrn) {
        assertThat(select(from(OfflineContent.TABLE)
                .whereEq(OfflineContent._ID, playlistUrn.getNumericId())
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)), counts(1));
    }

    public void assertPlaylistNotMarkedForOfflineSync(Urn playlistUrn) {
        assertThat(select(from(OfflineContent.TABLE)
                .whereEq(OfflineContent._ID, playlistUrn.getNumericId())
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST)), counts(0));
    }

    public void assertOfflineLikesDisabled() {
        assertThat(select(from(OfflineContent.TABLE)
                .whereEq(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION)), counts(0));
    }

    public void assertOfflineLikesEnabled() {
        assertThat(select(from(OfflineContent.TABLE)
                .whereEq(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES)
                .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION)), counts(1));
    }

    public void assertWaveformForTrack(Urn track, WaveformData waveformData) {
        WaveformSerializer serializer = new WaveformSerializer();
        assertThat(select(from(Table.Waveforms.name())
                .whereEq(TableColumns.Waveforms.TRACK_ID, track.getNumericId())
                .whereEq(TableColumns.Waveforms.MAX_AMPLITUDE, waveformData.maxAmplitude)
                .whereEq(TableColumns.Waveforms.SAMPLES, serializer.serialize(waveformData.samples))), counts(1));
    }

}
