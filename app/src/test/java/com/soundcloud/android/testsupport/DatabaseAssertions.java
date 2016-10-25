package com.soundcloud.android.testsupport;

import static android.provider.BaseColumns._ID;
import static android.text.TextUtils.join;
import static com.soundcloud.android.activities.ActivityKind.PLAYLIST_LIKE;
import static com.soundcloud.android.activities.ActivityKind.PLAYLIST_REPOST;
import static com.soundcloud.android.activities.ActivityKind.TRACK_COMMENT;
import static com.soundcloud.android.activities.ActivityKind.TRACK_LIKE;
import static com.soundcloud.android.activities.ActivityKind.TRACK_REPOST;
import static com.soundcloud.android.activities.ActivityKind.USER_FOLLOW;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECENT;
import static com.soundcloud.android.stations.StationsCollectionsTypes.RECOMMENDATIONS;
import static com.soundcloud.android.storage.Table.Activities;
import static com.soundcloud.android.storage.Table.Likes;
import static com.soundcloud.android.storage.Table.PlaylistTracks;
import static com.soundcloud.android.storage.Table.Posts;
import static com.soundcloud.android.storage.Table.PromotedTracks;
import static com.soundcloud.android.storage.Table.SoundView;
import static com.soundcloud.android.storage.Table.Sounds;
import static com.soundcloud.android.storage.Table.TrackPolicies;
import static com.soundcloud.android.storage.Table.UserAssociations;
import static com.soundcloud.android.storage.Table.Users;
import static com.soundcloud.android.storage.Table.Waveforms;
import static com.soundcloud.android.storage.TableColumns.Activities.COMMENT_ID;
import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_ID;
import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_TYPE;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.PLAYLIST_ID;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.REMOVED_AT;
import static com.soundcloud.android.storage.TableColumns.PlaylistTracks.TRACK_ID;
import static com.soundcloud.android.storage.TableColumns.Posts.TARGET_TYPE;
import static com.soundcloud.android.storage.TableColumns.Posts.TYPE_REPOST;
import static com.soundcloud.android.storage.TableColumns.PromotedTracks.AD_URN;
import static com.soundcloud.android.storage.TableColumns.PromotedTracks.PROMOTER_ID;
import static com.soundcloud.android.storage.TableColumns.ResourceTable.CREATED_AT;
import static com.soundcloud.android.storage.TableColumns.ResourceTable._TYPE;
import static com.soundcloud.android.storage.TableColumns.SoundView.USERNAME;
import static com.soundcloud.android.storage.TableColumns.Sounds.ARTWORK_URL;
import static com.soundcloud.android.storage.TableColumns.Sounds.COMMENTABLE;
import static com.soundcloud.android.storage.TableColumns.Sounds.COMMENT_COUNT;
import static com.soundcloud.android.storage.TableColumns.Sounds.DURATION;
import static com.soundcloud.android.storage.TableColumns.Sounds.FULL_DURATION;
import static com.soundcloud.android.storage.TableColumns.Sounds.GENRE;
import static com.soundcloud.android.storage.TableColumns.Sounds.LIKES_COUNT;
import static com.soundcloud.android.storage.TableColumns.Sounds.PERMALINK_URL;
import static com.soundcloud.android.storage.TableColumns.Sounds.PLAYBACK_COUNT;
import static com.soundcloud.android.storage.TableColumns.Sounds.RELEASE_DATE;
import static com.soundcloud.android.storage.TableColumns.Sounds.REPOSTS_COUNT;
import static com.soundcloud.android.storage.TableColumns.Sounds.SET_TYPE;
import static com.soundcloud.android.storage.TableColumns.Sounds.SHARING;
import static com.soundcloud.android.storage.TableColumns.Sounds.SNIPPET_DURATION;
import static com.soundcloud.android.storage.TableColumns.Sounds.STREAM_URL;
import static com.soundcloud.android.storage.TableColumns.Sounds.TAG_LIST;
import static com.soundcloud.android.storage.TableColumns.Sounds.TITLE;
import static com.soundcloud.android.storage.TableColumns.Sounds.TRACK_COUNT;
import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.storage.TableColumns.Sounds.TYPE_TRACK;
import static com.soundcloud.android.storage.TableColumns.Sounds.USER_ID;
import static com.soundcloud.android.storage.TableColumns.Sounds.WAVEFORM_URL;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.BLOCKED;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.MONETIZABLE;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.MONETIZATION_MODEL;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.POLICY;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.SNIPPED;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.SUB_HIGH_TIER;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.SUB_MID_TIER;
import static com.soundcloud.android.storage.TableColumns.TrackPolicies.SYNCABLE;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.ADDED_AT;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.ASSOCIATION_TYPE;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.RESOURCE_TYPE;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.TARGET_ID;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.TYPE_FOLLOWING;
import static com.soundcloud.android.storage.TableColumns.UserAssociations.TYPE_RESOURCE_USER;
import static com.soundcloud.android.storage.TableColumns.Users.ARTIST_STATION;
import static com.soundcloud.android.storage.TableColumns.Users.AVATAR_URL;
import static com.soundcloud.android.storage.TableColumns.Users.COUNTRY;
import static com.soundcloud.android.storage.TableColumns.Users.DESCRIPTION;
import static com.soundcloud.android.storage.TableColumns.Users.DISCOGS_NAME;
import static com.soundcloud.android.storage.TableColumns.Users.FOLLOWERS_COUNT;
import static com.soundcloud.android.storage.TableColumns.Users.MYSPACE_NAME;
import static com.soundcloud.android.storage.TableColumns.Users.VISUAL_URL;
import static com.soundcloud.android.storage.TableColumns.Users.WEBSITE_NAME;
import static com.soundcloud.android.storage.TableColumns.Users.WEBSITE_URL;
import static com.soundcloud.android.storage.TableColumns.Waveforms.MAX_AMPLITUDE;
import static com.soundcloud.android.storage.TableColumns.Waveforms.SAMPLES;
import static com.soundcloud.android.storage.Tables.Comments.BODY;
import static com.soundcloud.android.storage.Tables.Comments.TIMESTAMP;
import static com.soundcloud.android.storage.Tables.Comments.URN;
import static com.soundcloud.android.storage.Tables.OfflineContent.ID_OFFLINE_LIKES;
import static com.soundcloud.android.storage.Tables.OfflineContent.TYPE_COLLECTION;
import static com.soundcloud.android.storage.Tables.Stations.ARTWORK_URL_TEMPLATE;
import static com.soundcloud.android.storage.Tables.Stations.LAST_PLAYED_TRACK_POSITION;
import static com.soundcloud.android.storage.Tables.Stations.PERMALINK;
import static com.soundcloud.android.storage.Tables.Stations.STATION_URN;
import static com.soundcloud.android.storage.Tables.Stations.TYPE;
import static com.soundcloud.android.storage.Tables.StationsCollections.COLLECTION_TYPE;
import static com.soundcloud.android.storage.Tables.TrackDownloads.DOWNLOADED_AT;
import static com.soundcloud.android.storage.Tables.TrackDownloads.TABLE;
import static com.soundcloud.android.storage.Tables.TrackDownloads.UNAVAILABLE_AT;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.assertions.QueryAssertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.api.model.stream.ApiPromotedPlaylist;
import com.soundcloud.android.api.model.stream.ApiPromotedTrack;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.comments.CommentRecord;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.policies.ApiPolicyInfo;
import com.soundcloud.android.stations.ApiStation;
import com.soundcloud.android.stations.ApiStationMetadata;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.ChartTracks;
import com.soundcloud.android.storage.Tables.Charts;
import com.soundcloud.android.storage.Tables.Comments;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.PlayHistory;
import com.soundcloud.android.storage.Tables.RecentlyPlayed;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsCollections;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.storage.Tables.SuggestedCreators;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiChartBucket;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.android.waveform.WaveformData;
import com.soundcloud.android.waveform.WaveformSerializer;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.test.assertions.QueryBinding;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.Date;
import java.util.List;

public class DatabaseAssertions {

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

    public void assertTrackInserted(TrackRecord track) {
        final Query query = from(Sounds.name())
                .whereEq(_ID, track.getUrn().getNumericId())
                .whereEq(_TYPE, TYPE_TRACK)
                .whereEq(TITLE, track.getTitle())
                .whereEq(SNIPPET_DURATION, track.getSnippetDuration())
                .whereEq(FULL_DURATION, track.getFullDuration())
                .whereEq(WAVEFORM_URL, track.getWaveformUrl())
                .whereEq(STREAM_URL, track.getStreamUrl())
                .whereEq(PERMALINK_URL, track.getPermalinkUrl())
                .whereEq(ARTWORK_URL, track.getImageUrlTemplate().orNull())
                .whereEq(CREATED_AT, track.getCreatedAt().getTime())
                .whereEq(GENRE, track.getGenre())
                .whereEq(SHARING, track.getSharing().value())
                .whereEq(USER_ID, track.getUser().getUrn().getNumericId())
                .whereEq(COMMENTABLE, track.isCommentable())
                .whereEq(LIKES_COUNT, track.getLikesCount())
                .whereEq(REPOSTS_COUNT, track.getRepostsCount())
                .whereEq(PLAYBACK_COUNT, track.getPlaybackCount())
                .whereEq(COMMENT_COUNT, track.getCommentsCount());
        if (track.getDescription().isPresent()) {
            query.whereEq(DESCRIPTION, track.getDescription().get());
        }
        assertThat(select(query)).counts(1);
        assertTrackPolicyInserted(track);
    }

    public void assertTrackIsUnavailable(Urn trackUrn, long time) {
        assertThat(select(from(TABLE)
                                  .whereEq(TrackDownloads._ID, trackUrn.getNumericId())
                                  .whereEq(UNAVAILABLE_AT, time))).counts(1);
    }

    public void assertDownloadIsAvailable(Urn track) {
        assertThat(select(from(TABLE)
                                  .whereEq(TrackDownloads._ID, track.getNumericId())
                                  .whereNull(UNAVAILABLE_AT))).counts(1);
    }

    public void assertPlaylistTrackForAddition(Urn playlist, Urn track) {
        assertThat(select(from(PlaylistTracks.name())
                                  .whereEq(PLAYLIST_ID, playlist.getNumericId())
                                  .whereEq(TRACK_ID, track.getNumericId())
                                  .whereNotNull(ADDED_AT))).counts(1);
    }

    public void assertNoPlaylistTrackForAddition(Urn playlist, Urn track) {
        assertThat(select(from(PlaylistTracks.name())
                                  .whereEq(PLAYLIST_ID, playlist.getNumericId())
                                  .whereEq(TRACK_ID, track.getNumericId())
                                  .whereNull(ADDED_AT))).counts(1);
    }

    public void assertPlaylistTrackForRemoval(Urn playlist, Urn track) {
        assertThat(select(from(PlaylistTracks.name())
                                  .whereEq(PLAYLIST_ID, playlist.getNumericId())
                                  .whereEq(TRACK_ID, track.getNumericId())
                                  .whereNotNull(REMOVED_AT))).counts(1);
    }

    public void assertUserFollowingsPending(Urn targetUrn, boolean following) {
        Query query = from(UserAssociations.name())
                .whereEq(TARGET_ID, targetUrn.getNumericId())
                .whereEq(ASSOCIATION_TYPE, TYPE_FOLLOWING)
                .whereEq(RESOURCE_TYPE, TYPE_RESOURCE_USER)
                .whereNotNull(TableColumns.UserAssociations.CREATED_AT);

        if (following) {
            query.whereNotNull(ADDED_AT)
                 .whereNull(TableColumns.UserAssociations.REMOVED_AT);
        } else {
            query.whereNull(ADDED_AT)
                 .whereNotNull(TableColumns.UserAssociations.REMOVED_AT);
        }

        assertThat(select(query)).counts(1);
    }

    public void assertUserFollowersCount(Urn targetUrn, int numberOfFollowers) {
        assertThat(select(from(Users.name())
                                  .whereEq(_ID, targetUrn.getNumericId())
                                  .whereEq(FOLLOWERS_COUNT, numberOfFollowers))).counts(1);
    }


    public void assertStationMetadataInserted(ApiStationMetadata station, int lastPlayedTrackPosition) {
        assertThat(select(
                from(Stations.TABLE)
                        .whereEq(STATION_URN, station.getUrn())
                        .whereEq(Stations.TITLE, station.getTitle())
                        .whereEq(TYPE, station.getType())
                        .whereEq(PERMALINK, station.getPermalink())
                        .whereEq(LAST_PLAYED_TRACK_POSITION, lastPlayedTrackPosition)
        )).counts(1);
    }

    public void assertStationMetadataInserted(ApiStationMetadata station) {
        final Optional<String> artworkUrlTemplateOptional = station.getArtworkUrlTemplate();
        final String artworkUrlTemplate = artworkUrlTemplateOptional.isPresent() ?
                                          artworkUrlTemplateOptional.get() :
                                          null;
        assertThat(select(
                from(Stations.TABLE)
                        .whereEq(STATION_URN, station.getUrn())
                        .whereEq(Stations.TITLE, station.getTitle())
                        .whereEq(TYPE, station.getType())
                        .whereEq(PERMALINK, station.getPermalink())
                        .whereEq(ARTWORK_URL_TEMPLATE, artworkUrlTemplate)
                        .whereNull(LAST_PLAYED_TRACK_POSITION)
                        .whereNotNull(Stations.PLAY_QUEUE_UPDATED_AT)
        )).counts(1);
    }

    private void assertStationPlayQueueInserted(StationRecord station) {
        assertThat(select(from(StationsPlayQueues.TABLE)
                                  .whereEq(StationsPlayQueues.STATION_URN, station.getUrn().toString())
                                  .whereIn(TRACK_ID, Lists.transform(station.getTracks(), StationTrack.TO_TRACK_IDS))
        )).counts(station.getTracks().size());
    }

    public void assertStationInserted(ApiStation station) {
        assertStationMetadataInserted(station.getMetadata());
        assertStationPlayQueueInserted(station);
    }

    public void assertStationIsUnique(Urn station) {
        assertThat(select(from(Stations.TABLE).whereEq(STATION_URN, station))).counts(1);
    }

    public void assertStationUpdateTime(Urn station, long lastUpdateTime) {
        assertThat(select(from(Stations.TABLE)
                                  .whereEq(STATION_URN, station)
                                  .whereEq(Stations.PLAY_QUEUE_UPDATED_AT, lastUpdateTime))).counts(1);
    }

    public void assertStationPlayQueuePosition(Urn station, int position) {
        assertThat(select(
                from(Stations.TABLE)
                        .whereEq(STATION_URN, station.toString())
                        .whereEq(LAST_PLAYED_TRACK_POSITION, position)
        )).counts(1);
    }

    public void assertStationPlayQueuePositionNotSet(Urn station) {
        assertThat(select(
                from(Stations.TABLE)
                        .whereEq(STATION_URN, station.toString())
                        .whereNull(LAST_PLAYED_TRACK_POSITION)
        )).counts(1);
    }


    public void assertStationPlayQueueContains(Urn stationUrn, List<StationTrack> stationTracks) {
        assertThat(
                select(from(StationsPlayQueues.TABLE)
                               .whereEq(StationsPlayQueues.STATION_URN, stationUrn.toString())))
                .counts(stationTracks.size());

        for (int position = 0; position < stationTracks.size(); position++) {
            StationTrack stationTrack = stationTracks.get(position);
            assertThat(
                    select(from(StationsPlayQueues.TABLE)
                                   .whereEq(StationsPlayQueues.STATION_URN, stationUrn.toString())
                                   .whereEq(StationsPlayQueues.POSITION, position)
                                   .whereEq(StationsPlayQueues.TRACK_ID, stationTrack.getTrackUrn().getNumericId())
                                   .whereEq(StationsPlayQueues.QUERY_URN, stationTrack.getQueryUrn().toString())))
                    .counts(1);
        }
    }

    public void assertRecentStationsContains(Urn stationUrn, long currentTime, int expectedCount) {
        assertThat(select(from(StationsCollections.TABLE)
                                  .whereEq(StationsCollections.STATION_URN, stationUrn.toString())
                                  .whereEq(COLLECTION_TYPE, RECENT)
                                  .whereEq(StationsCollections.ADDED_AT, currentTime))).counts(expectedCount);
    }

    public void assertRecentStationsAtPosition(Urn stationUrn, long position) {
        assertThat(select(from(StationsCollections.TABLE)
                                  .whereEq(StationsCollections.STATION_URN, stationUrn.toString())
                                  .whereEq(StationsCollections.POSITION, position))).counts(1);
    }

    public void assertNoStationsCollections() {
        assertThat(select(from(StationsCollections.TABLE))).counts(0);
    }

    public void assertNoStations() {
        assertThat(select(from(Stations.TABLE))).counts(0);
    }

    public void assertNoStationPlayQueues() {
        assertThat(select(from(StationsPlayQueues.TABLE))).counts(0);
    }

    public void assertLocalStationLike(Urn stationUrn) {
        assertThat(select(from(StationsCollections.TABLE)
                                  .whereEq(StationsCollections.STATION_URN, stationUrn.toString())
                                  .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED)
                                  .whereNull(StationsCollections.REMOVED_AT)
                                  .whereNotNull(StationsCollections.ADDED_AT)))
                .counts(1);
    }

    public void assertLikedStationHasSize(int number) {
        assertThat(select(from(StationsCollections.TABLE)
                .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED)))
                .counts(number);
    }

    public void assertLocalStationUnlike(Urn stationUrn) {
        assertThat(select(from(StationsCollections.TABLE)
                                  .whereEq(StationsCollections.STATION_URN, stationUrn.toString())
                                  .whereEq(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED)
                                  .whereNull(StationsCollections.ADDED_AT)
                                  .whereNotNull(StationsCollections.REMOVED_AT)))
                .counts(1);

    }

    public void assertLocalStationDeleted(Urn urn) {
        assertThat(select(from(StationsCollections.TABLE)
                                  .whereEq(StationsCollections.STATION_URN, urn.toString())
                                  .whereNull(StationsCollections.ADDED_AT))).counts(1);
    }

    public void assertRecommendedStationsEquals(List<Urn> stations) {
        assertThat(select(from(StationsCollections.TABLE)
                                  .whereIn(StationsCollections.STATION_URN, stations)
                                  .whereEq(COLLECTION_TYPE, RECOMMENDATIONS))).counts(stations.size());
    }

    public void assertPlayHistory(PlayHistoryRecord record, int count) {
        assertThat(select(from(PlayHistory.TABLE)
                                  .whereEq(PlayHistory.TRACK_ID, record.trackUrn().getNumericId())
                                  .whereEq(PlayHistory.TIMESTAMP, record.timestamp()))).counts(count);
    }

    public void assertPlayHistory(PlayHistoryRecord record) {
        assertPlayHistory(record, 1);
    }

    public void assertPlayHistoryCount(int count) {
        assertThat(select(from(PlayHistory.TABLE))).counts(count);
    }

    public void assertRecentlyPlayed(PlayHistoryRecord record, int count) {
        assertThat(select(from(RecentlyPlayed.TABLE)
                                  .whereEq(RecentlyPlayed.CONTEXT_ID, record.contextUrn().getNumericId())
                                  .whereEq(RecentlyPlayed.CONTEXT_TYPE, record.getContextType())
                                  .whereEq(RecentlyPlayed.TIMESTAMP, record.timestamp()))).counts(count);
    }

    public void assertRecentlyPlayed(PlayHistoryRecord record) {
        assertRecentlyPlayed(record, 1);
    }

    public void assertRecentlyPlayedCount(int count) {
        assertThat(select(from(RecentlyPlayed.TABLE))).counts(count);
    }

    private void assertTrackPolicyInserted(TrackRecord track) {
        final Query query = from(TrackPolicies.name())
                .whereEq(TableColumns.TrackPolicies.TRACK_ID, track.getUrn().getNumericId())
                .whereEq(MONETIZABLE, track.isMonetizable())
                .whereEq(BLOCKED, track.isBlocked())
                .whereEq(SNIPPED, track.isSnipped())
                .whereEq(SYNCABLE, track.isSyncable())
                .whereEq(POLICY, track.getPolicy());

        if (track.getMonetizationModel().isPresent()) {
            query.whereEq(MONETIZATION_MODEL, track.getMonetizationModel().get());
        }
        if (track.isSubMidTier().isPresent()) {
            query.whereEq(SUB_MID_TIER, track.isSubMidTier().get());
        }
        if (track.isSubHighTier().isPresent()) {
            query.whereEq(SUB_HIGH_TIER, track.isSubHighTier().get());
        }
        assertThat(select(query)).counts(1);
    }

    public void assertPolicyInserted(ApiPolicyInfo apiPolicyInfo) {
        final Query query = from(TrackPolicies.name())
                .whereEq(TableColumns.TrackPolicies.TRACK_ID, apiPolicyInfo.getUrn().getNumericId())
                .whereEq(MONETIZABLE, apiPolicyInfo.isMonetizable())
                .whereEq(SYNCABLE, apiPolicyInfo.isSyncable())
                .whereEq(POLICY, apiPolicyInfo.getPolicy())
                .whereEq(SNIPPED, apiPolicyInfo.isSnipped())
                .whereEq(BLOCKED, apiPolicyInfo.isBlocked())
                .whereEq(MONETIZATION_MODEL, apiPolicyInfo.getMonetizationModel())
                .whereEq(SUB_MID_TIER, apiPolicyInfo.isSubMidTier())
                .whereEq(SUB_HIGH_TIER, apiPolicyInfo.isSubHighTier());

        assertThat(select(query)).counts(1);
    }

    public void assertLikeInserted(ApiLike like) {
        assertLikeInserted(like.toPropertySet());
    }

    public void assertLikeInserted(PropertySet like) {
        assertLikeInserted(like.get(LikeProperty.TARGET_URN), like.get(LikeProperty.CREATED_AT));
    }

    public void assertLikeInserted(Urn targetUrn, Date createdAt) {
        assertThat(select(from(Likes.name())
                                  .whereEq(_ID, targetUrn.getNumericId())
                                  .whereEq(TableColumns.Likes._TYPE, targetUrn.isTrack()
                                                                     ? TYPE_TRACK : TYPE_PLAYLIST)
                                  .whereEq(TableColumns.Likes.CREATED_AT, createdAt.getTime()))).counts(1);
    }

    public void assertLikeActivityInserted(Urn targetUrn, Urn userUrn, Date createdAt) {
        assertThat(select(from(Activities.name())
                                  .whereEq(TableColumns.Activities.TYPE, targetUrn.isTrack() ?
                                                                         TRACK_LIKE.identifier() :
                                                                         PLAYLIST_LIKE.identifier())
                                  .whereEq(SOUND_ID, targetUrn.getNumericId())
                                  .whereEq(SOUND_TYPE, targetUrn.isTrack()
                                                       ? TYPE_TRACK : TYPE_PLAYLIST)
                                  .whereEq(TableColumns.Activities.USER_ID, userUrn.getNumericId())
                                  .whereEq(TableColumns.Activities.CREATED_AT, createdAt.getTime()))).counts(1);
    }

    public void assertLikedTrackPendingAddition(Urn targetUrn) {
        assertLikedPendingAddition(targetUrn, TableColumns.Sounds.TYPE_TRACK);
    }

    public void assertLikedPlaylistPendingAddition(Urn targetUrn) {
        assertLikedPendingAddition(targetUrn, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    public void assertLikesCount(Urn urn, int newLikesCount) {
        assertThat(select(from(SoundView.name())
                                  .whereEq(_ID, urn.getNumericId())
                                  .whereEq(_TYPE, urn.isTrack()
                                                  ? TYPE_TRACK : TYPE_PLAYLIST)
                                  .whereEq(TableColumns.SoundView.LIKES_COUNT, newLikesCount))).counts(1);
    }

    public void assertRepostCount(Urn urn, int newRepostCount) {
        assertThat(select(from(SoundView.name())
                                  .whereEq(_ID, urn.getNumericId())
                                  .whereEq(_TYPE, urn.isTrack()
                                                  ? TYPE_TRACK : TYPE_PLAYLIST)
                                  .whereEq(TableColumns.SoundView.REPOSTS_COUNT, newRepostCount))).counts(1);
    }

    private void assertLikedPendingAddition(Urn targetUrn, int type) {
        assertThat(select(from(Likes.name())
                                  .whereEq(_ID, targetUrn.getNumericId())
                                  .whereEq(TableColumns.Likes._TYPE, type)
                                  .whereNotNull(TableColumns.Likes.ADDED_AT)
                                  .whereNull(TableColumns.Likes.REMOVED_AT))).counts(1);
    }

    public void assertLikedTrackPendingRemoval(Urn targetUrn) {
        assertLikedPendingRemoval(targetUrn, TableColumns.Sounds.TYPE_TRACK);
    }

    public void assertLikedPlaylistPendingRemoval(Urn targetUrn) {
        assertLikedPendingRemoval(targetUrn, TableColumns.Sounds.TYPE_PLAYLIST);
    }

    private void assertLikedPendingRemoval(Urn targetUrn, int type) {
        assertThat(select(from(Likes.name())
                                  .whereEq(_ID, targetUrn.getNumericId())
                                  .whereEq(TableColumns.Likes._TYPE, type)
                                  .whereNull(TableColumns.Likes.ADDED_AT)
                                  .whereNotNull(TableColumns.Likes.REMOVED_AT))).counts(1);
    }

    public void assertPlayableUserInserted(UserRecord user) {
        assertThat(select(from(SoundView.name())
                                  .whereEq(TableColumns.SoundView.USER_ID, user.getUrn().getNumericId())
                                  .whereEq(USERNAME, user.getUsername())
        )).counts(1);
    }

    public void assertPlaylistNotStored(Urn urn) {
        assertPlaylistNotStored(urn.getNumericId());
    }

    public void assertPlaylistNotStored(long playlistId) {
        assertThat(select(from(Sounds.name())
                                  .whereEq(_ID, playlistId)
                                  .whereEq(_TYPE, TYPE_PLAYLIST))).counts(0);
    }

    public void assertPlaylistTracksNotStored(Urn playlistUrn) {
        assertThat(select(from(PlaylistTracks.name())
                                  .whereEq(TableColumns.PlaylistTracks.PLAYLIST_ID,
                                           playlistUrn.getNumericId()))).counts(0);
    }

    public void assertPlaylistInserted(Urn playlist) {
        assertPlaylistInserted(playlist.getNumericId());
    }

    public void assertPlaylistInserted(long playlistId) {
        assertThat(select(from(Sounds.name())
                                  .whereEq(_ID, playlistId)
                                  .whereEq(_TYPE, TYPE_PLAYLIST))).counts(1);
    }

    public void assertPlaylistInserted(ApiPlaylist playlist) {
        assertThat(select(from(Sounds.name())
                                  .whereEq(_ID, playlist.getId())
                                  .whereEq(_TYPE, TYPE_PLAYLIST)
                                  .whereEq(TITLE, playlist.getTitle())
                                  .whereEq(DURATION, playlist.getDuration())
                                  .whereEq(CREATED_AT, playlist.getCreatedAt().getTime())
                                  .whereEq(PERMALINK_URL, playlist.getPermalinkUrl())
                                  .whereEq(ARTWORK_URL, playlist.getImageUrlTemplate().orNull())
                                  .whereEq(SHARING, playlist.getSharing().value())
                                  .whereEq(USER_ID, playlist.getUser().getId())
                                  .whereEq(LIKES_COUNT, playlist.getStats().getLikesCount())
                                  .whereEq(REPOSTS_COUNT, playlist.getStats().getRepostsCount())
                                  .whereEq(TRACK_COUNT, playlist.getTrackCount())
                                  .whereEq(GENRE, playlist.getGenre())
                                  .whereEq(TAG_LIST, join(" ", playlist.getTags())))).counts(1);
    }

    public void assertPlaylistInserted(long playlistId, String title, boolean isPrivate) {
        assertThat(select(from(Sounds.name())
                                  .whereEq(_ID, playlistId)
                                  .whereEq(_TYPE, TYPE_PLAYLIST)
                                  .whereEq(USER_ID, 321L)
                                  .whereNotNull(CREATED_AT)
                                  .whereEq(SHARING, Sharing.from(!isPrivate).value())
                                  .whereEq(TITLE, title)
                                  .whereEq(SET_TYPE, Strings.EMPTY)
                                  .whereEq(RELEASE_DATE, Strings.EMPTY))).counts(1);
    }

    public void assertModifiedPlaylistInserted(Urn playlistUrn, String title, boolean isPrivate) {
        assertThat(select(from(Sounds.name())
                                  .whereEq(_ID, playlistUrn.getNumericId())
                                  .whereEq(_TYPE, TYPE_PLAYLIST)
                                  .whereNotNull(CREATED_AT)
                                  .whereEq(SHARING, Sharing.from(!isPrivate).value())
                                  .whereEq(TITLE, title)
                                  .whereNotNull(TableColumns.Sounds.MODIFIED_AT))).counts(1);
    }

    public void assertPlaylistTracklist(long playlistId, List<Urn> tracklist) {
        for (int i = 0; i < tracklist.size(); i++) {
            assertThat(select(from(PlaylistTracks.name())
                                      .whereEq(PLAYLIST_ID, playlistId)
                                      .whereEq(TRACK_ID, tracklist.get(i).getNumericId())
                                      .whereEq(TableColumns.PlaylistTracks.POSITION, i))).counts(1);
        }

        // assert no additional tracks
        assertThat(select(from(PlaylistTracks.name())
                                  .whereEq(PLAYLIST_ID, playlistId)
                                  .whereNull(REMOVED_AT)
                                  .whereGe(TableColumns.PlaylistTracks.POSITION, tracklist.size()))).counts(0);
    }

    public void assertPlaylistPostInsertedFor(ApiPlaylist playlist) {
        assertPlaylistPostInsertedFor(playlist.getUrn());
    }

    public void assertPlaylistPostInsertedFor(Urn playlistUrn) {
        assertThat(select(from(Posts.name())
                                  .whereEq(TableColumns.Posts.TARGET_ID, playlistUrn.getNumericId())
                                  .whereEq(TARGET_TYPE, TYPE_PLAYLIST))).counts(1);
    }

    public void assertPlaylistLikeInsertedFor(ApiPlaylist playlist) {
        assertPlaylistLikeInsertedFor(playlist.getUrn());
    }

    private void assertPlaylistLikeInsertedFor(Urn playlistUrn) {
        assertThat(select(from(Likes.name())
                                  .whereEq(TableColumns.Likes._ID, playlistUrn.getNumericId())
                                  .whereEq(TableColumns.Likes._TYPE, TYPE_PLAYLIST))).counts(1);
    }

    public void assertTrackRepostInserted(Urn urn, Date createdAt) {
        assertThat(select(from(Posts.name())
                                  .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                                  .whereEq(TARGET_TYPE, TYPE_TRACK)
                                  .whereEq(TableColumns.Posts.CREATED_AT, createdAt.getTime())
                                  .whereEq(TableColumns.Posts.TYPE, TYPE_REPOST))).counts(1);
    }

    public void assertTrackRepostNotExistent(Urn urn) {
        assertThat(select(from(Posts.name())
                                  .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                                  .whereEq(TARGET_TYPE, TYPE_TRACK)
                                  .whereEq(TableColumns.Posts.TYPE, TYPE_REPOST))).counts(0);
    }

    public void assertPlaylistRepostInserted(Urn urn, Date createdAt) {
        assertThat(select(from(Posts.name())
                                  .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                                  .whereEq(TARGET_TYPE, TYPE_PLAYLIST)
                                  .whereEq(TableColumns.Posts.CREATED_AT, createdAt.getTime())
                                  .whereEq(TableColumns.Posts.TYPE, TYPE_REPOST))).counts(1);
    }

    public void assertPlaylistRepostNotExistent(Urn urn) {
        assertThat(select(from(Posts.name())
                                  .whereEq(TableColumns.Posts.TARGET_ID, urn.getNumericId())
                                  .whereEq(TARGET_TYPE, TYPE_PLAYLIST)
                                  .whereEq(TableColumns.Posts.TYPE, TYPE_REPOST))).counts(0);
    }

    public void assertRepostActivityInserted(Urn targetUrn, Urn userUrn, Date createdAt) {
        assertThat(select(from(Activities.name())
                                  .whereEq(TableColumns.Activities.TYPE, targetUrn.isTrack() ?
                                                                         TRACK_REPOST.identifier() :
                                                                         PLAYLIST_REPOST.identifier())
                                  .whereEq(SOUND_ID, targetUrn.getNumericId())
                                  .whereEq(SOUND_TYPE, targetUrn.isTrack()
                                                       ? TYPE_TRACK : TYPE_PLAYLIST)
                                  .whereEq(TableColumns.Activities.USER_ID, userUrn.getNumericId())
                                  .whereEq(TableColumns.Activities.CREATED_AT, createdAt.getTime()))).counts(1);
    }

    public void assertCommentActivityInserted(long commentId, Urn trackUrn, Urn commenterUrn, Date createdAt) {
        assertThat(select(from(Activities.name())
                                  .whereEq(TableColumns.Activities.TYPE, TRACK_COMMENT.identifier())
                                  .whereEq(SOUND_ID, trackUrn.getNumericId())
                                  .whereEq(SOUND_TYPE, TYPE_TRACK)
                                  .whereEq(TableColumns.Activities.USER_ID, commenterUrn.getNumericId())
                                  .whereEq(COMMENT_ID, commentId)
                                  .whereEq(TableColumns.Activities.CREATED_AT, createdAt.getTime()))).counts(1);
    }

    public void assertFollowActivityInserted(Urn followerUrn, Date createdAt) {
        assertThat(select(from(Activities.name())
                                  .whereEq(TableColumns.Activities.TYPE, USER_FOLLOW.identifier())
                                  .whereEq(TableColumns.Activities.USER_ID, followerUrn.getNumericId())
                                  .whereEq(TableColumns.Activities.CREATED_AT, createdAt.getTime()))).counts(1);
    }

    public void assertPromotionInserted(ApiPromotedTrack promotedTrack) {
        assertThat(select(attachPromotedTrackingQueries(
                from(PromotedTracks.name())
                        .whereEq(AD_URN, promotedTrack.getAdUrn())
                        .whereEq(PROMOTER_ID, promotedTrack.getPromoter().getId())
                        .whereNotNull(TableColumns.PromotedTracks.CREATED_AT),
                promotedTrack)
        )).counts(1);
    }

    public void assertPromotionInserted(ApiPromotedPlaylist promotedPlaylist) {
        assertThat(select(attachPromotedTrackingQueries(
                from(PromotedTracks.name())
                        .whereEq(AD_URN, promotedPlaylist.getAdUrn())
                        .whereEq(PROMOTER_ID, promotedPlaylist.getPromoter().getId())
                        .whereNotNull(TableColumns.PromotedTracks.CREATED_AT),
                promotedPlaylist)
        )).counts(1);
    }

    public void assertPromotionWithoutPromoterInserted(ApiPromotedTrack promotedTrack) {
        assertThat(select(attachPromotedTrackingQueries(
                from(PromotedTracks.name())
                        .whereEq(AD_URN, promotedTrack.getAdUrn())
                        .whereNull(PROMOTER_ID)
                        .whereNotNull(TableColumns.PromotedTracks.CREATED_AT),
                promotedTrack)
        )).counts(1);
    }

    public void assertPromotionWithoutPromoterInserted(ApiPromotedPlaylist promotedPlaylist) {
        assertThat(select(attachPromotedTrackingQueries(
                from(PromotedTracks.name())
                        .whereEq(AD_URN, promotedPlaylist.getAdUrn())
                        .whereNull(PROMOTER_ID)
                        .whereNotNull(TableColumns.PromotedTracks.CREATED_AT),
                promotedPlaylist)
        )).counts(1);
    }

    private Query attachPromotedTrackingQueries(Query query, ApiPromotedTrack promotedTrack) {
        return query.whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS,
                             TextUtils.join(" ", promotedTrack.getTrackingTrackPlayedUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS,
                             TextUtils.join(" ", promotedTrack.getTrackingTrackClickedUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS,
                             TextUtils.join(" ", promotedTrack.getTrackingTrackImpressionUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_PROFILE_CLICKED_URLS,
                             TextUtils.join(" ", promotedTrack.getTrackingProfileClickedUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS,
                             TextUtils.join(" ", promotedTrack.getTrackingPromoterClickedUrls()));
    }

    private Query attachPromotedTrackingQueries(Query query, ApiPromotedPlaylist promotedPlaylist) {
        return query.whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS,
                             TextUtils.join(" ", promotedPlaylist.getTrackingTrackPlayedUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS,
                             TextUtils.join(" ", promotedPlaylist.getTrackingPlaylistClickedUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS,
                             TextUtils.join(" ", promotedPlaylist.getTrackingPlaylistImpressionUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_PROFILE_CLICKED_URLS,
                             TextUtils.join(" ", promotedPlaylist.getTrackingProfileClickedUrls()))
                    .whereEq(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS,
                             TextUtils.join(" ", promotedPlaylist.getTrackingPromoterClickedUrls()));
    }

    public void assertUserInserted(UserRecord user) {
        final Query query = from(Users.name())
                .whereEq(_ID, user.getUrn().getNumericId())
                .whereEq(TableColumns.Users.USERNAME, user.getUsername())
                .whereEq(TableColumns.Users.PERMALINK, user.getPermalink())
                .whereEq(COUNTRY, user.getCountry())
                .whereEq(FOLLOWERS_COUNT, user.getFollowersCount());

        assertOptionalColumn(query, DESCRIPTION, user.getDescription());
        assertOptionalColumn(query, WEBSITE_URL, user.getWebsiteUrl());
        assertOptionalColumn(query, AVATAR_URL, user.getImageUrlTemplate());
        assertOptionalColumn(query, VISUAL_URL, user.getVisualUrlTemplate());
        assertOptionalColumn(query, WEBSITE_NAME, user.getWebsiteName());
        assertOptionalColumn(query, DISCOGS_NAME, user.getDiscogsName());
        assertOptionalColumn(query, MYSPACE_NAME, user.getMyspaceName());
        assertOptionalColumn(query, ARTIST_STATION, user.getArtistStationUrn());
        assertThat(select(query)).counts(1);
    }

    private <T> void assertOptionalColumn(Query query, String column, Optional<T> optional) {
        if (optional.isPresent()) {
            query.whereEq(column, String.valueOf(optional.get()));
        } else {
            query.whereNull(column);
        }
    }

    public void assertDownloadRequestsInserted(List<Urn> tracksToDownload) {
        for (Urn urn : tracksToDownload) {
            assertThat(select(from(TABLE)
                                      .whereEq(TrackDownloads._ID, urn.getNumericId()))).counts(1);
        }
    }

    public void assertDownloadPendingRemoval(Urn trackUrn) {
        assertThat(select(from(TABLE)
                                  .whereEq(TrackDownloads._ID, trackUrn.getNumericId())
                                  .whereNotNull(DOWNLOADED_AT)
                                  .whereNotNull(TrackDownloads.REMOVED_AT))).counts(1);
    }

    public void assertDownloadResultsInserted(DownloadState result) {
        assertThat(select(from(TABLE)
                                  .whereNull(UNAVAILABLE_AT)
                                  .whereEq(TrackDownloads._ID, result.getTrack().getNumericId())
                                  .whereEq(DOWNLOADED_AT, result.getTimestamp()))).counts(1);
    }

    public void assertNotDownloaded(Urn trackUrn) {
        assertThat(select(from(TABLE)
                                  .whereEq(TrackDownloads._ID, trackUrn.getNumericId()))).counts(0);
    }

    public void assertDownloadedAndNotMarkedForRemoval(Urn trackUrn) {
        assertThat(select(from(TABLE)
                                  .whereEq(TrackDownloads._ID, trackUrn.getNumericId())
                                  .whereNotNull(DOWNLOADED_AT)
                                  .whereNull(TrackDownloads.REMOVED_AT))).counts(1);
    }

    protected QueryBinding select(Query query) {
        return new QueryBinding(this.database, query);
    }

    public void assertIsOfflinePlaylist(Urn playlistUrn) {
        assertThat(select(from(OfflineContent.TABLE)
                                  .whereEq(OfflineContent._ID, playlistUrn.getNumericId())
                                  .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST))).counts(1);
    }

    public void assertIsNotOfflinePlaylist(Urn playlistUrn) {
        assertThat(select(from(OfflineContent.TABLE)
                                  .whereEq(OfflineContent._ID, playlistUrn.getNumericId())
                                  .whereEq(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST))).counts(0);
    }

    public void assertLikedTracksIsNotOffline() {
        assertThat(select(from(OfflineContent.TABLE)
                                  .whereEq(OfflineContent._ID, ID_OFFLINE_LIKES)
                                  .whereEq(OfflineContent._TYPE, TYPE_COLLECTION))).counts(0);
    }

    public void assertLikedTracksIsOffline() {
        assertThat(select(from(OfflineContent.TABLE)
                                  .whereEq(OfflineContent._ID, ID_OFFLINE_LIKES)
                                  .whereEq(OfflineContent._TYPE, TYPE_COLLECTION))).counts(1);
    }

    public void assertWaveformForTrack(Urn track, WaveformData waveformData) {
        WaveformSerializer serializer = new WaveformSerializer();
        assertThat(select(from(Waveforms.name())
                                  .whereEq(TableColumns.Waveforms.TRACK_ID, track.getNumericId())
                                  .whereEq(MAX_AMPLITUDE, waveformData.maxAmplitude)
                                  .whereEq(SAMPLES, serializer.serialize(waveformData.samples)))).counts(1);
    }

    public void assertCommentInserted(CommentRecord comment) {
        assertThat(select(from(Comments.TABLE)
                                  .whereEq(URN, comment.getUrn().toString())
                                  .whereEq(Comments.TRACK_ID, comment.getTrackUrn().getNumericId())
                                  .whereEq(Comments.USER_ID, comment.getUser().getUrn().getNumericId())
                                  .whereEq(TIMESTAMP, comment.getTrackTime())
                                  .whereEq(Comments.CREATED_AT, comment.getCreatedAt().getTime())
                                  .whereEq(BODY, comment.getBody()))).counts(1);
    }

    public void assertChartInserted(ApiChartBucket apiChartBucket) {
        final ApiChart<ApiImageResource> apiChart = apiChartBucket.getCharts().get(0);
        assertThat(select(from(Charts.TABLE)
                                  .whereEq(Charts.GENRE, apiChart.genre())
                                  .whereEq(Charts.CATEGORY, apiChart.category())
                                  .whereEq(Charts.BUCKET_TYPE, apiChartBucket.getBucketType())
                                  .whereEq(Charts.DISPLAY_NAME, apiChart.displayName())
                                  .whereEq(Charts.TYPE, apiChart.type())));
        ApiImageResource track = apiChart.tracks().getCollection().get(0);
        assertThat(select(from(ChartTracks.TABLE)
                                  .whereEq(ChartTracks.TRACK_ID, track.getUrn().getNumericId())
                                  .whereEq(ChartTracks.TRACK_ARTWORK, track.getImageUrlTemplate())));
    }

    public void assertChartRemoved(ApiChartBucket apiChartBucket) {
        final ApiChart<ApiImageResource> apiChart = apiChartBucket.getCharts().get(0);
        assertThat(select(from(Charts.TABLE)
                                  .whereEq(Charts.GENRE, apiChart.genre())
                                  .whereEq(Charts.CATEGORY, apiChart.category())
                                  .whereEq(Charts.BUCKET_TYPE, apiChartBucket.getBucketType())
                                  .whereEq(Charts.DISPLAY_NAME, apiChart.displayName())
                                  .whereEq(Charts.TYPE, apiChart.type()))).counts(0);
        ApiImageResource track = apiChart.tracks().getCollection().get(0);
        assertThat(select(from(ChartTracks.TABLE)
                                  .whereEq(ChartTracks.TRACK_ID, track.getUrn().getNumericId())
                                  .whereEq(ChartTracks.TRACK_ARTWORK, track.getImageUrlTemplate()))).counts(0);
    }

    public void assertSuggestedCreatorInserted(ApiSuggestedCreator apiSuggestedCreator) {
        assertThat(select(from(SuggestedCreators.TABLE)
                                  .whereEq(SuggestedCreators.RELATION_KEY, apiSuggestedCreator.getRelationKey())
                                  .whereEq(SuggestedCreators.SEED_USER_ID, apiSuggestedCreator.getSeedUser().getId())
                                  .whereEq(SuggestedCreators.SUGGESTED_USER_ID,
                                           apiSuggestedCreator.getSuggestedUser().getId()))).counts(1);
        assertUserInserted(apiSuggestedCreator.getSeedUser());
        assertUserInserted(apiSuggestedCreator.getSuggestedUser());
    }

    public void assertSuggestedCreatorRemoved(ApiSuggestedCreator apiSuggestedCreator) {
        assertThat(select(from(SuggestedCreators.TABLE)
                                  .whereEq(SuggestedCreators.RELATION_KEY, apiSuggestedCreator.getRelationKey())
                                  .whereEq(SuggestedCreators.SEED_USER_ID, apiSuggestedCreator.getSeedUser().getId())
                                  .whereEq(SuggestedCreators.SUGGESTED_USER_ID,
                                           apiSuggestedCreator.getSuggestedUser().getId()))).counts(0);
    }

    public void assertNoSuggestedCreators() {
        assertThat(select(from(SuggestedCreators.TABLE))).counts(0);
    }
}
