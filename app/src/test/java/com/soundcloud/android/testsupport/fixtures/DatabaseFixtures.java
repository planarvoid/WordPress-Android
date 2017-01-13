package com.soundcloud.android.testsupport.fixtures;

import static com.soundcloud.android.discovery.charts.ChartsFixtures.createChartWithImageResources;

import com.soundcloud.android.Consts;
import com.soundcloud.android.activities.ActivityKind;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.collection.playhistory.PlayHistoryRecord;
import com.soundcloud.android.comments.ApiComment;
import com.soundcloud.android.discovery.recommendedplaylists.ApiRecommendedPlaylistBucket;
import com.soundcloud.android.discovery.charts.Chart;
import com.soundcloud.android.discovery.charts.ChartBucketType;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.ApiStation;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.stations.StationRecord;
import com.soundcloud.android.stations.StationTrack;
import com.soundcloud.android.stations.StationsCollectionsTypes;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsCollections;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.sync.activities.ApiPlaylistRepostActivity;
import com.soundcloud.android.sync.activities.ApiTrackCommentActivity;
import com.soundcloud.android.sync.activities.ApiTrackLikeActivity;
import com.soundcloud.android.sync.activities.ApiUserFollowActivity;
import com.soundcloud.android.sync.charts.ApiChart;
import com.soundcloud.android.sync.charts.ApiImageResource;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.sync.posts.ApiPost;
import com.soundcloud.android.sync.suggestedCreators.ApiSuggestedCreator;
import com.soundcloud.android.tracks.TrackArtwork;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.propeller.ContentValuesBuilder;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatabaseFixtures {

    private final SQLiteDatabase database;

    public DatabaseFixtures(SQLiteDatabase database) {
        this.database = database;
    }

    public ApiTrack insertTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertTrack(track);
        return track;
    }

    public ApiTrack insertBlockedTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setBlocked(true);
        insertTrack(track);
        return track;
    }

    public void insertTrack(ApiTrack track) {
        insertUser(track.getUser());
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.Sounds._ID, track.getUrn().getNumericId());
        cv.put(Tables.Sounds.TITLE, track.getTitle());
        cv.put(Tables.Sounds._TYPE, Tables.Sounds.TYPE_TRACK);
        cv.put(Tables.Sounds.USER_ID, track.getUser().getUrn().getNumericId());
        cv.put(Tables.Sounds.SNIPPET_DURATION, track.getSnippetDuration());
        cv.put(Tables.Sounds.FULL_DURATION, track.getFullDuration());
        cv.put(Tables.Sounds.WAVEFORM_URL, track.getWaveformUrl());
        cv.put(Tables.Sounds.ARTWORK_URL, track.getImageUrlTemplate().orNull());
        cv.put(Tables.Sounds.STREAM_URL, track.getStreamUrl());
        cv.put(Tables.Sounds.LIKES_COUNT, track.getStats().getLikesCount());
        cv.put(Tables.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount());
        cv.put(Tables.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount());
        cv.put(Tables.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount());
        cv.put(Tables.Sounds.PERMALINK_URL, track.getPermalinkUrl());
        cv.put(Tables.Sounds.SHARING, track.getSharing().value());
        cv.put(Tables.Sounds.CREATED_AT, track.getCreatedAt().getTime());
        cv.put(Tables.Sounds.DESCRIPTION, track.getDescription().orNull());
        cv.put(Tables.Sounds.GENRE, track.getGenre());

        insertInto(Tables.Sounds.TABLE, cv.get());
        insertPolicy(track);
    }

    public void insertFollowing(Urn followedUrn, int position) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.UserAssociations.ASSOCIATION_TYPE, Tables.UserAssociations.TYPE_FOLLOWING);
        cv.put(Tables.UserAssociations.RESOURCE_TYPE, Tables.UserAssociations.TYPE_RESOURCE_USER);
        cv.put(Tables.UserAssociations.TARGET_ID, followedUrn.getNumericId());
        cv.put(Tables.UserAssociations.CREATED_AT, System.currentTimeMillis());
        cv.put(Tables.UserAssociations.POSITION, position);
        insertInto(Tables.UserAssociations.TABLE, cv.get());
    }

    public void insertFollowing(Urn followedUrn) {
        insertFollowing(followedUrn, System.currentTimeMillis());
    }

    public void insertFollowing(Urn followedUrn, long follwedAt) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.UserAssociations.ASSOCIATION_TYPE, Tables.UserAssociations.TYPE_FOLLOWING);
        cv.put(Tables.UserAssociations.RESOURCE_TYPE, Tables.UserAssociations.TYPE_RESOURCE_USER);
        cv.put(Tables.UserAssociations.TARGET_ID, followedUrn.getNumericId());
        cv.put(Tables.UserAssociations.CREATED_AT, follwedAt);
        cv.put(Tables.UserAssociations.POSITION, 0);
        insertInto(Tables.UserAssociations.TABLE, cv.get());
    }

    public void insertFollowingPendingRemoval(Urn followedUrn, long removedAt) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.UserAssociations.ASSOCIATION_TYPE, Tables.UserAssociations.TYPE_FOLLOWING);
        cv.put(Tables.UserAssociations.RESOURCE_TYPE, Tables.UserAssociations.TYPE_RESOURCE_USER);
        cv.put(Tables.UserAssociations.TARGET_ID, followedUrn.getNumericId());
        cv.put(Tables.UserAssociations.CREATED_AT, System.currentTimeMillis());
        cv.put(Tables.UserAssociations.REMOVED_AT, removedAt);
        insertInto(Tables.UserAssociations.TABLE, cv.get());
    }

    public void insertFollowingPendingAddition(Urn followedUrn, long addedAt) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.UserAssociations.ASSOCIATION_TYPE, Tables.UserAssociations.TYPE_FOLLOWING);
        cv.put(Tables.UserAssociations.RESOURCE_TYPE, Tables.UserAssociations.TYPE_RESOURCE_USER);
        cv.put(Tables.UserAssociations.TARGET_ID, followedUrn.getNumericId());
        cv.put(Tables.UserAssociations.CREATED_AT, System.currentTimeMillis());
        cv.put(Tables.UserAssociations.ADDED_AT, addedAt);
        insertInto(Tables.UserAssociations.TABLE, cv.get());
    }

    public void insertFollower(Urn userUrn, int position) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.UserAssociations.ASSOCIATION_TYPE, Tables.UserAssociations.TYPE_FOLLOWER);
        cv.put(Tables.UserAssociations.RESOURCE_TYPE, Tables.UserAssociations.TYPE_RESOURCE_USER);
        cv.put(Tables.UserAssociations.TARGET_ID, userUrn.getNumericId());
        cv.put(Tables.UserAssociations.CREATED_AT, System.currentTimeMillis());
        cv.put(Tables.UserAssociations.POSITION, position);
        insertInto(Tables.UserAssociations.TABLE, cv.get());
    }

    public void insertRecentlyPlayedStationAtPosition(Urn stationUrn, long position) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(StationsCollections.STATION_URN, stationUrn.toString());
        cv.put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT);
        cv.put(StationsCollections.POSITION, position);
        cv.put(StationsCollections.ADDED_AT, null);
        insertInto(StationsCollections.TABLE, cv.get());
    }

    public void insertRecentlyPlayedUnsyncedStation(Urn stationUrn, long time) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(StationsCollections.STATION_URN, stationUrn.toString());
        cv.put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT);
        cv.put(StationsCollections.POSITION, Consts.NOT_SET);
        cv.put(StationsCollections.ADDED_AT, time);
        insertInto(StationsCollections.TABLE, cv.get());
    }

    public void insertLocallyPlayedRecentStation(Urn stationUrn, long time) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(StationsCollections.STATION_URN, stationUrn.toString());
        cv.put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.RECENT);
        cv.put(StationsCollections.ADDED_AT, time);
        insertInto(StationsCollections.TABLE, cv.get());
    }

    public void insertPlayHistory(long timestamp, Urn urn) {
        ContentValues cv = new ContentValues();
        cv.put(Tables.PlayHistory.TIMESTAMP.name(), timestamp);
        cv.put(Tables.PlayHistory.TRACK_ID.name(), urn.getNumericId());
        cv.put(Tables.PlayHistory.SYNCED.name(), true);
        insertInto(Tables.PlayHistory.TABLE, cv);
    }

    public void insertRecentlyPlayed(long timestamp, Urn urn) {
        ContentValues cv = new ContentValues();
        PlayHistoryRecord record = PlayHistoryRecord.create(timestamp, Urn.NOT_SET, urn);
        cv.put(Tables.RecentlyPlayed.TIMESTAMP.name(), record.timestamp());
        cv.put(Tables.RecentlyPlayed.CONTEXT_TYPE.name(), record.getContextType());
        cv.put(Tables.RecentlyPlayed.CONTEXT_ID.name(), record.contextUrn().getNumericId());
        insertInto(Tables.RecentlyPlayed.TABLE, cv);
    }

    public void insertUnsyncedPlayHistory(long timestamp, Urn urn) {
        ContentValues cv = new ContentValues();
        cv.put(Tables.PlayHistory.TIMESTAMP.name(), timestamp);
        cv.put(Tables.PlayHistory.TRACK_ID.name(), urn.getNumericId());
        cv.put(Tables.PlayHistory.SYNCED.name(), false);
        insertInto(Tables.PlayHistory.TABLE, cv);
    }

    public void insertSuggestedCreator(ApiSuggestedCreator apiSuggestedCreator) {
        insertUser(apiSuggestedCreator.getSeedUser());
        insertUser(apiSuggestedCreator.getSuggestedUser());
        ContentValues cv = new ContentValues();
        cv.put(Tables.SuggestedCreators.RELATION_KEY.name(), apiSuggestedCreator.getRelationKey());
        cv.put(Tables.SuggestedCreators.SEED_USER_ID.name(), apiSuggestedCreator.getSeedUser().getId());
        cv.put(Tables.SuggestedCreators.SUGGESTED_USER_ID.name(), apiSuggestedCreator.getSuggestedUser().getId());
        insertInto(Tables.SuggestedCreators.TABLE, cv);
    }

    public void insertRecommendedPlaylist(ApiRecommendedPlaylistBucket playlistBucket) {
        insertPlaylists(playlistBucket.playlists());
        final ContentValues contentValues = new ContentValues();
        contentValues.put(Tables.RecommendedPlaylistBucket.KEY.name(), playlistBucket.key());
        contentValues.put(Tables.RecommendedPlaylistBucket.DISPLAY_NAME.name(), playlistBucket.displayName());
        contentValues.put(Tables.RecommendedPlaylistBucket.ARTWORK_URL.name(), playlistBucket.artworkUrl().orNull());
        long bucketId = insertInto(Tables.RecommendedPlaylistBucket.TABLE, contentValues);

        for (ApiPlaylist apiPlaylist : playlistBucket.playlists()) {
            final ContentValues matcherValues = new ContentValues();
            matcherValues.put(Tables.RecommendedPlaylist.BUCKET_ID.name(), bucketId);
            matcherValues.put(Tables.RecommendedPlaylist.PLAYLIST_ID.name(), apiPlaylist.getId());
            insertInto(Tables.RecommendedPlaylist.TABLE, matcherValues);
        }
    }

    private void insertPolicy(ApiTrack track) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.TrackPolicies.TRACK_ID, track.getId());
        cv.put(Tables.TrackPolicies.POLICY, track.getPolicy());
        cv.put(Tables.TrackPolicies.MONETIZABLE, track.isMonetizable());
        cv.put(Tables.TrackPolicies.BLOCKED, track.isBlocked());
        cv.put(Tables.TrackPolicies.SNIPPED, track.isSnipped());
        cv.put(Tables.TrackPolicies.SYNCABLE, track.isSyncable());
        cv.put(Tables.TrackPolicies.SUB_HIGH_TIER, track.isSubHighTier().get());
        cv.put(Tables.TrackPolicies.SUB_MID_TIER, track.isSubMidTier().get());
        cv.put(Tables.TrackPolicies.MONETIZATION_MODEL, track.getMonetizationModel().get());
        cv.put(Tables.TrackPolicies.LAST_UPDATED, System.currentTimeMillis());

        insertInto(Tables.TrackPolicies.TABLE, cv.get());
    }

    public void insertDescription(Urn trackUrn, String description) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.Sounds._ID, trackUrn.getNumericId());
        cv.put(Tables.Sounds._TYPE, Tables.Sounds.TYPE_TRACK);
        cv.put(Tables.Sounds.DESCRIPTION, description);
        insertInto(Tables.Sounds.TABLE, cv.get());
    }

    public ApiPlaylist insertPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiPlaylist insertEmptyPlaylist() {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setTrackCount(0);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiPlaylist insertPlaylistWithCreatedAt(Date createdAt) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setCreatedAt(createdAt);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiPlaylist insertPlaylistAlbum(String setType, String releaseDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setIsAlbum(true);
        playlist.setSetType(setType);
        playlist.setReleaseDate(releaseDate);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiPlaylist insertLocalPlaylist() {
        return insertLocalPlaylist(ModelFixtures.create(ApiPlaylist.class));
    }

    public ApiPlaylist insertLocalPlaylist(ApiPlaylist playlist) {
        playlist.setUrn(Urn.forPlaylist(-(1000 + playlist.getId())));
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public void insertPlaylists(List<ApiPlaylist> playlists) {
        for (ApiPlaylist playlist : playlists) {
            insertPlaylist(playlist);
        }
    }

    public void insertPlaylist(ApiPlaylist playlist) {
        insertInto(Tables.Sounds.TABLE, createPlaylistContentValues(playlist).get());
    }

    public ApiTrack insertPlaylistTrackWithPolicyHighTierMonetizable(ApiPlaylist playlist, int position) {
        final ApiTrack apiTrack = insertPlaylistTrack(playlist.getUrn(), position);
        return insertPolicyHighTierMonetizable(apiTrack);
    }

    private ApiTrack insertPolicyHighTierMonetizable(ApiTrack apiTrack) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.TrackPolicies.TRACK_ID, apiTrack.getUrn().getNumericId());
        cv.put(Tables.TrackPolicies.POLICY, "SNIP");
        cv.put(Tables.TrackPolicies.MONETIZABLE, true);
        cv.put(Tables.TrackPolicies.MONETIZATION_MODEL, "SUB_HIGH_TIER");
        cv.put(Tables.TrackPolicies.SUB_MID_TIER, false);
        cv.put(Tables.TrackPolicies.SUB_HIGH_TIER, true);
        cv.put(Tables.TrackPolicies.SYNCABLE, false);
        insertInto(Tables.TrackPolicies.TABLE, cv.get());

        apiTrack.setPolicy("SNIP");
        apiTrack.setMonetizable(true);
        apiTrack.setMonetizationModel("SUB_HIGH_TIER");
        apiTrack.setSubMidTier(false);
        apiTrack.setSubHighTier(true);
        apiTrack.setSyncable(false);

        return apiTrack;
    }

    public ApiTrack insertPlaylistTrack(ApiPlaylist playlist, int position) {
        return insertPlaylistTrack(playlist.getUrn(), position);
    }

    public ApiTrack insertPlaylistTrack(Urn playlistUrn, int position) {
        ApiTrack apiTrack = insertTrack();
        insertPlaylistTrack(playlistUrn, apiTrack.getUrn(), position);
        return apiTrack;
    }

    public void insertPlaylistTrack(Urn playlistUrn, Urn trackUrn, int position) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PlaylistTracks.PLAYLIST_ID, playlistUrn.getNumericId());
        cv.put(TableColumns.PlaylistTracks.TRACK_ID, trackUrn.getNumericId());
        cv.put(TableColumns.PlaylistTracks.POSITION, position);
        insertInto(Table.PlaylistTracks, cv);
    }

    public ApiTrack insertPlaylistTrackPendingAddition(ApiPlaylist playlist, int position, Date additionDate) {
        final ApiTrack apiTrack = insertPlaylistTrack(playlist, position);
        database.execSQL("UPDATE PlaylistTracks SET added_at=" + additionDate.getTime()
                                 + " WHERE playlist_id=" + playlist.getId()
                                 + " AND track_id=" + apiTrack.getId());
        return apiTrack;
    }

    public ApiTrack insertPlaylistTrackPendingRemoval(ApiPlaylist playlist, int position, Date removalDate) {
        final ApiTrack apiTrack = insertPlaylistTrack(playlist, position);
        database.execSQL("UPDATE PlaylistTracks SET removed_at=" + removalDate.getTime()
                                 + " WHERE playlist_id=" + playlist.getId()
                                 + " AND track_id=" + apiTrack.getId());
        return apiTrack;
    }

    public ApiPlaylist insertPlaylistWithCreationDate(ApiUser user, Date createdAtDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        playlist.setCreatedAt(createdAtDate);
        playlist.setUser(user);
        insertPlaylist(playlist);
        return playlist;
    }

    public ApiTrack insertTrackWithTitle(String title) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setTitle(title);
        insertTrack(track);
        return track;
    }

    public ApiTrack insertTrackWithCreationDate(ApiUser user, Date createdAtDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setCreatedAt(createdAtDate);
        track.setUser(user);
        insertTrack(track);
        return track;
    }

    public ApiTrack insertPrivateTrackWithCreationDate(ApiUser user, Date createdAtDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setCreatedAt(createdAtDate);
        track.setUser(user);
        track.setSharing(Sharing.PRIVATE);
        insertTrack(track);
        return track;
    }

    public ApiTrack insertTrackWithUser(ApiTrack track, ApiUser user) {
        track.setUser(user);
        insertTrack(track);
        return track;
    }

    public ApiUser insertUser() {
        final ApiUser user = ModelFixtures.create(ApiUser.class);
        insertUser(user);
        return user;
    }

    public void insertUser(UserRecord user) {
        insertInto(Tables.Users.TABLE, basicUserContentValues(user).get());
    }

    @NonNull
    private ContentValuesBuilder basicUserContentValues(UserRecord user) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.Users._ID, user.getUrn().getNumericId());
        cv.put(Tables.Users.USERNAME, user.getUsername());
        cv.put(Tables.Users.FIRST_NAME, user.getFirstName().orNull());
        cv.put(Tables.Users.LAST_NAME, user.getLastName().orNull());
        cv.put(Tables.Users.SIGNUP_DATE, user.getCreatedAt().isPresent() ? user.getCreatedAt().get().getTime() : null);
        cv.put(Tables.Users.COUNTRY, user.getCountry());
        cv.put(Tables.Users.CITY, user.getCity());
        cv.put(Tables.Users.FOLLOWERS_COUNT, user.getFollowersCount());
        cv.put(Tables.Users.AVATAR_URL, user.getImageUrlTemplate().orNull());
        cv.put(Tables.Users.VISUAL_URL, user.getVisualUrlTemplate().orNull());
        return cv;
    }

    public void insertExtendedUser(ApiUser user,
                                   String description,
                                   String websiteUrl,
                                   String websiteTitle,
                                   String discogsName,
                                   String myspaceName) {

        ContentValuesBuilder cv = basicUserContentValues(user);
        cv.put(Tables.Users.DESCRIPTION, description);
        cv.put(Tables.Users.WEBSITE_URL, websiteUrl);
        cv.put(Tables.Users.WEBSITE_NAME, websiteTitle);
        cv.put(Tables.Users.DISCOGS_NAME, discogsName);
        cv.put(Tables.Users.MYSPACE_NAME, myspaceName);

        insertInto(Tables.Users.TABLE, cv.get());
    }

    public void insertUser(ApiUser user,
                           Urn artistStation) {
        ContentValuesBuilder cv = basicUserContentValues(user);
        cv.put(Tables.Users.ARTIST_STATION, artistStation.toString());

        insertInto(Tables.Users.TABLE, cv.get());
    }

    public void insertLike(long id, int type, Date createdAt) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.Likes._ID, id);
        cv.put(Tables.Likes._TYPE, type);
        cv.put(Tables.Likes.CREATED_AT, createdAt.getTime());
        insertInto(Tables.Likes.TABLE, cv.get());
    }

    public ApiStation insertStation() {
        return insertStation(StationFixtures.getApiStation());
    }

    public ApiStation insertStation(ApiStation station) {
        insertInto(Stations.TABLE, getDefaultStationContentValuesBuilder(station, System.currentTimeMillis()).get());
        insertStationPlayQueue(station);
        return station;
    }

    public ApiStation insertStation(int lastPlayedPosition) {
        return insertStation(StationFixtures.getApiStation(), lastPlayedPosition);
    }

    private ApiStation insertStation(ApiStation station, int lastPlayedPosition) {
        return insertStation(station, System.currentTimeMillis(), lastPlayedPosition);
    }

    public ApiStation insertStation(ApiStation station, long createdAt, int lastPlayedPosition) {
        insertInto(Stations.TABLE, getStationContentValues(station, createdAt, lastPlayedPosition));
        insertStationPlayQueue(station);
        return station;
    }

    public ApiStation insertLikedStation() {
        final ApiStation apiStation = insertStation();
        insertInto(StationsCollections.TABLE, getStationLikeCollectionContentValues(apiStation.getUrn(), true));
        return apiStation;
    }

    public ApiStation insertLocalLikedStation() {
        return insertLocalLikedStation(System.currentTimeMillis());
    }

    public ApiStation insertLocalLikedStation(long date) {
        final ApiStation apiStation = insertStation();
        insertInto(StationsCollections.TABLE, getStationLikeCollectionContentValues(apiStation.getUrn(), true, date));
        return apiStation;
    }

    public ApiStation insertLocalUnlikedStation() {
        return insertLocalUnlikedStation(System.currentTimeMillis());
    }

    public ApiStation insertLocalUnlikedStation(long date) {
        final ApiStation apiStation = insertStation();
        insertInto(StationsCollections.TABLE, getStationLikeCollectionContentValues(apiStation.getUrn(), false, date));
        return apiStation;
    }

    public ApiStation insertUnlikedStation() {
        final ApiStation apiStation = insertStation();
        insertInto(StationsCollections.TABLE, getStationLikeCollectionContentValues(apiStation.getUrn(), false));
        return apiStation;
    }

    private ContentValues getStationLikeCollectionContentValues(Urn stationUrn, boolean isLiked) {
        return getStationLikeCollectionContentValues(stationUrn, isLiked, -1);
    }

    private ContentValues getStationLikeCollectionContentValues(Urn stationUrn, boolean isLiked, long localChangeDate) {
        final ContentValuesBuilder builder = ContentValuesBuilder
                .values()
                .put(StationsCollections.STATION_URN, stationUrn.toString())
                .put(StationsCollections.COLLECTION_TYPE, StationsCollectionsTypes.LIKED);

        if (localChangeDate >= 0) {
            builder
                    .put(isLiked ? StationsCollections.ADDED_AT : StationsCollections.REMOVED_AT, localChangeDate)
                    .put(isLiked ? StationsCollections.REMOVED_AT : StationsCollections.ADDED_AT, null);
        }

        return builder.get();
    }

    public Chart insertChart(ChartType chartType, ChartCategory chartCategory) {
        final ApiChart<ApiImageResource> chart = createChartWithImageResources(chartType, chartCategory);
        return insertChart(chart, Tables.Charts.BUCKET_TYPE_GLOBAL);
    }

    public Chart insertChart(ApiChart<ApiImageResource> apiChart, int bucketType) {
        final ContentValues cv = new ContentValues();
        cv.put(Tables.Charts.DISPLAY_NAME.name(), apiChart.displayName());
        if (apiChart.genre() != null) {
            cv.put(Tables.Charts.GENRE.name(), apiChart.genre().toString());
        }
        cv.put(Tables.Charts.TYPE.name(), apiChart.type().value());
        cv.put(Tables.Charts.CATEGORY.name(), apiChart.category().value());
        cv.put(Tables.Charts.BUCKET_TYPE.name(), bucketType);
        long chartLocalId = insertInto(Tables.Charts.TABLE, cv);

        final List<ApiImageResource> apiChartTracks = apiChart.tracks().getCollection();
        final List<TrackArtwork> trackArtworks = new ArrayList<>(apiChartTracks.size());
        for (final ApiImageResource track : apiChartTracks) {
            trackArtworks.add(insertChartTrack(track, chartLocalId));
        }
        return Chart.create(chartLocalId,
                            apiChart.type(),
                            apiChart.category(),
                            apiChart.displayName(),
                            apiChart.genre(),
                            getChartBucketType(bucketType),
                            trackArtworks);
    }

    private ChartBucketType getChartBucketType(int bucketType) {
        switch (bucketType) {
            case Tables.Charts.BUCKET_TYPE_FEATURED_GENRES:
                return ChartBucketType.FEATURED_GENRES;
            default:
                return ChartBucketType.GLOBAL;
        }
    }

    private TrackArtwork insertChartTrack(ApiImageResource imageResource, long chartLocalId) {
        final ContentValues cv = new ContentValues();
        cv.put(Tables.ChartTracks.CHART_ID.name(), chartLocalId);
        cv.put(Tables.ChartTracks.TRACK_ID.name(), imageResource.getUrn().getNumericId());
        if (imageResource.getImageUrlTemplate().isPresent()) {
            cv.put(Tables.ChartTracks.TRACK_ARTWORK.name(), imageResource.getImageUrlTemplate().get());
        }
        insertInto(Tables.ChartTracks.TABLE, cv);
        return TrackArtwork.create(imageResource.getUrn(), imageResource.getImageUrlTemplate());
    }

    private ContentValues getStationContentValues(StationRecord station, long createdAt, int lastPlayedPosition) {
        return getDefaultStationContentValuesBuilder(station, createdAt)
                .put(Stations.LAST_PLAYED_TRACK_POSITION, lastPlayedPosition)
                .get();
    }

    private void insertStationPlayQueue(ApiStation station) {
        final List<StationTrack> stationTracks = station.getTracks();

        for (int i = 0; i < stationTracks.size(); i++) {
            final StationTrack track = stationTracks.get(i);
            insertInto(StationsPlayQueues.TABLE, getTrackContentValues(i, station, track));
            insertTrack(station.getTrackRecords().get(i));
        }
    }

    private ContentValuesBuilder getDefaultStationContentValuesBuilder(StationRecord station, long createdAt) {
        return ContentValuesBuilder.values()
                                   .put(Stations.STATION_URN, station.getUrn().toString())
                                   .put(Stations.TITLE, station.getTitle())
                                   .put(Stations.TYPE, station.getType())
                                   .put(Stations.PLAY_QUEUE_UPDATED_AT, createdAt)
                                   .put(Stations.PERMALINK, station.getPermalink())
                                   .put(Stations.ARTWORK_URL_TEMPLATE, station.getImageUrlTemplate().orNull());
    }

    private ContentValues getTrackContentValues(int position, StationRecord stationInfo, StationTrack track) {
        final ContentValuesBuilder trackContentValues = ContentValuesBuilder.values();

        trackContentValues.put(StationsPlayQueues.POSITION, position);
        trackContentValues.put(StationsPlayQueues.STATION_URN, stationInfo.getUrn().toString());
        trackContentValues.put(StationsPlayQueues.TRACK_ID, track.getTrackUrn().getNumericId());
        trackContentValues.put(StationsPlayQueues.QUERY_URN, track.getQueryUrn().toString());

        return trackContentValues.get();
    }

    public void insertLike(ApiLike like) {
        insertLike(
                like.getTargetUrn().getNumericId(),
                like.getTargetUrn().isTrack() ? Tables.Sounds.TYPE_TRACK : Tables.Sounds.TYPE_PLAYLIST,
                like.getCreatedAt());
    }

    public ApiLike insertTrackLike() {
        final ApiLike like = ModelFixtures.apiTrackLike();
        insertLike(like);
        return like;
    }

    public ApiTrack insertLikedTrack(Date likedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertUser(track.getUser());
        insertTrack(track);
        insertLike(track.getId(), Tables.Sounds.TYPE_TRACK, likedDate);
        return track;
    }

    public ApiTrack insertLikedTrackPendingRemoval(Date unlikedDate) {
        return insertLikedTrackPendingRemoval(new Date(0), unlikedDate);
    }

    public ApiTrack insertLikedTrackPendingRemoval(Date likedDate, Date unlikedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertUser(track.getUser());
        insertTrack(track);
        insertLike(track.getId(), Tables.Sounds.TYPE_TRACK, likedDate);
        database.execSQL("UPDATE Likes SET removed_at=" + unlikedDate.getTime()
                                 + " WHERE _id=" + track.getUrn().getNumericId()
                                 + " AND _type=" + Tables.Sounds.TYPE_TRACK);
        return track;
    }

    public ApiTrack insertLikedTrackPendingAddition(Date likedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertUser(track.getUser());
        insertTrack(track);
        insertLike(track.getId(), Tables.Sounds.TYPE_TRACK, likedDate);
        database.execSQL("UPDATE Likes SET added_at=" + likedDate.getTime()
                                 + " WHERE _id=" + track.getUrn().getNumericId()
                                 + " AND _type=" + Tables.Sounds.TYPE_TRACK);
        return track;
    }

    public ApiPlaylist insertModifiedPlaylist(Date modifiedDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        database.execSQL("UPDATE Sounds SET " + Tables.Sounds.MODIFIED_AT.name() + " = " + modifiedDate.getTime()
                                 + " WHERE _id=" + playlist.getUrn().getNumericId()
                                 + " AND _type=" + Tables.Sounds.TYPE_PLAYLIST);
        return playlist;
    }

    public ApiPlaylist insertLikedPlaylist(Date creationDate, Date likedDate) {
        return insertLikedPlaylist(likedDate, insertPlaylistWithCreatedAt(creationDate));
    }

    public ApiPlaylist insertLikedPlaylist(Date likedDate) {
        return insertLikedPlaylist(likedDate, insertPlaylist());
    }

    public ApiPlaylist insertLikedPlaylist(Date likedDate, ApiPlaylist playlist) {
        insertPlaylist(playlist);
        insertLike(playlist.getId(), Tables.Sounds.TYPE_PLAYLIST, likedDate);
        return playlist;
    }

    public ApiPlaylist insertPlaylistPendingRemoval() {
        ApiPlaylist playlist = insertPlaylist();

        ContentValuesBuilder cv = createPlaylistContentValues(playlist);
        cv.put(Tables.Sounds.REMOVED_AT, System.currentTimeMillis());
        insertInto(Tables.Sounds.TABLE, cv.get());

        return playlist;
    }

    @NonNull
    private ContentValuesBuilder createPlaylistContentValues(ApiPlaylist playlist) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.Sounds._ID, playlist.getId());
        cv.put(Tables.Sounds._TYPE, Tables.Sounds.TYPE_PLAYLIST);
        cv.put(Tables.Sounds.TITLE, playlist.getTitle());
        cv.put(Tables.Sounds.USER_ID, playlist.getUser().getId());
        cv.put(Tables.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount());
        cv.put(Tables.Sounds.REPOSTS_COUNT, playlist.getStats().getRepostsCount());
        cv.put(Tables.Sounds.PERMALINK_URL, playlist.getPermalinkUrl());
        cv.put(Tables.Sounds.SHARING, playlist.getSharing().value());
        cv.put(Tables.Sounds.DURATION, playlist.getDuration());
        cv.put(Tables.Sounds.TRACK_COUNT, playlist.getTrackCount());
        cv.put(Tables.Sounds.CREATED_AT, playlist.getCreatedAt().getTime());
        cv.put(Tables.Sounds.ARTWORK_URL, playlist.getImageUrlTemplate().orNull());
        cv.put(Tables.Sounds.TAG_LIST, TextUtils.join(" ", playlist.getTags()));
        cv.put(Tables.Sounds.IS_ALBUM, playlist.isAlbum());
        cv.put(Tables.Sounds.SET_TYPE, playlist.getSetType());
        cv.put(Tables.Sounds.RELEASE_DATE, playlist.getReleaseDate());
        cv.put(Tables.Sounds.GENRE, playlist.getGenre());
        return cv;
    }

    public ApiPlaylist insertLikedPlaylistPendingRemoval(Date unlikedDate) {
        return insertLikedPlaylistPendingRemoval(new Date(0), unlikedDate);
    }

    public ApiPlaylist insertLikedPlaylistPendingRemoval(Date likedDate, Date unlikedDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        insertLike(playlist.getId(), Tables.Sounds.TYPE_PLAYLIST, likedDate);
        database.execSQL("UPDATE Likes SET removed_at=" + unlikedDate.getTime()
                                 + " WHERE _id=" + playlist.getUrn().getNumericId()
                                 + " AND _type=" + Tables.Sounds.TYPE_PLAYLIST);
        return playlist;
    }

    public ApiPlaylist insertLikedPlaylistPendingAddition(Date likedDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        insertLike(playlist.getId(), Tables.Sounds.TYPE_PLAYLIST, likedDate);
        database.execSQL("UPDATE Likes SET added_at=" + likedDate.getTime()
                                 + " WHERE _id=" + playlist.getUrn().getNumericId()
                                 + " AND _type=" + Tables.Sounds.TYPE_PLAYLIST);
        return playlist;
    }

    public ApiPost insertTrackPost(ApiPost apiTrackPost) {
        insertTrackPost(apiTrackPost.getTargetUrn().getNumericId(),
                        apiTrackPost.getCreatedAt().getTime(),
                        false);
        return apiTrackPost;
    }

    public void insertTrackRepost(long id, long createdAt) {
        insertTrackPost(id, createdAt, true);
    }

    public void insertTrackPost(long id, long createdAt, boolean isRepost) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.Posts.TARGET_ID, id);
        cv.put(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_TRACK);
        cv.put(Tables.Posts.TYPE, isRepost ? Tables.Posts.TYPE_REPOST : Tables.Posts.TYPE_POST);
        cv.put(Tables.Posts.CREATED_AT, createdAt);
        insertInto(Tables.Posts.TABLE, cv.get());
    }

    public void insertPlaylistRepost(long id, long createdAt) {
        insertPlaylistPost(id, createdAt, true);
    }

    public void insertPlaylistPost(long playlistId, long createdAt, boolean isRepost) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.Posts.TARGET_ID, playlistId);
        cv.put(Tables.Posts.TARGET_TYPE, Tables.Sounds.TYPE_PLAYLIST);
        cv.put(Tables.Posts.TYPE, isRepost ? Tables.Posts.TYPE_REPOST : Tables.Posts.TYPE_POST);
        cv.put(Tables.Posts.CREATED_AT, createdAt);
        insertInto(Tables.Posts.TABLE, cv.get());
    }

    public ApiPlaylist insertPostedPlaylist(Date postedAt) {
        ApiPlaylist apiPlaylist = insertPlaylistWithCreatedAt(postedAt);
        insertPlaylistPost(apiPlaylist.getUrn().getNumericId(), postedAt.getTime(), false);
        return apiPlaylist;
    }

    public ApiTrack insertPostedTrack(Date postedAt, boolean isRepost) {
        ApiTrack apiTrack = insertTrack();
        insertTrackPost(apiTrack.getUrn().getNumericId(), postedAt.getTime(), isRepost);
        return apiTrack;
    }

    public void insertStreamTrackPost(ApiStreamItem apiStreamItem) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, apiStreamItem.getTrack().get().getId());
        cv.put(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, apiStreamItem.getTrack().get().getCreatedAt().getTime());
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamTrackPost(long trackId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamTrackRepost(long trackId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamPlaylistPost(long playlistId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamPlaylistRepost(long playlistId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        insertInto(Table.SoundStream, cv);
    }

    public ApiTrack insertPromotedStreamTrack(long timestamp) {
        ApiUser promoter = ModelFixtures.create(ApiUser.class);
        return insertPromotedStreamTrack(promoter, timestamp);
    }

    public ApiTrack insertPromotedStreamTrack(UserRecord promoter, long timestamp) {
        ApiTrack track = insertTrack();
        insertPromotedStreamTrack(track.getUrn(), promoter, timestamp, 0);
        return track;
    }

    public void insertPromotedStreamTrack(Urn trackUrn, UserRecord promoter, long timestamp, long promotedId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackUrn.getNumericId());
        cv.put(TableColumns.SoundStream.SOUND_TYPE, Tables.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.PROMOTED_ID, promotedId);
        insertInto(Table.SoundStream, cv);

        insertPromotedTrackMetadata(promoter, promotedId, timestamp);
    }

    public void insertPromotedTrackMetadata(long promotedId, long timestamp) {
        ApiUser promoter = ModelFixtures.create(ApiUser.class);
        insertPromotedTrackMetadata(promoter, promotedId, timestamp);
    }

    public void insertPromotedTrackMetadata(UserRecord promoter, long promotedId, long timestamp) {
        insertUser(promoter);
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PromotedTracks._ID, promotedId);
        cv.put(TableColumns.PromotedTracks.CREATED_AT, timestamp);
        cv.put(TableColumns.PromotedTracks.AD_URN, "promoted:track:123");
        cv.put(TableColumns.PromotedTracks.PROMOTER_ID, promoter.getUrn().getNumericId());
        cv.put(TableColumns.PromotedTracks.PROMOTER_NAME, promoter.getUsername());
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS, "promoted1 promoted2");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS, "promoted3 promoted4");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS, "promoted5 promoted6");
        cv.put(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS, "promoted7 promoted8");

        insertInto(Table.PromotedTracks, cv);
    }

    public void insertPolicyAllow(Urn urn, long lastUpdate) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(Tables.TrackPolicies.POLICY, "ALLOW");
        cv.put(Tables.TrackPolicies.MONETIZABLE, true);
        cv.put(Tables.TrackPolicies.SYNCABLE, true);
        cv.put(Tables.TrackPolicies.LAST_UPDATED, lastUpdate);

        insertInto(Tables.TrackPolicies.TABLE, cv.get());
    }

    public void insertPolicyBlock(Urn urn) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(Tables.TrackPolicies.POLICY, "BLOCK");
        cv.put(Tables.TrackPolicies.BLOCKED, true);
        cv.put(Tables.TrackPolicies.MONETIZABLE, false);
        cv.put(Tables.TrackPolicies.SYNCABLE, false);
        cv.put(Tables.TrackPolicies.SUB_MID_TIER, true);
        cv.put(Tables.TrackPolicies.SUB_HIGH_TIER, true);
        cv.put(Tables.TrackPolicies.SYNCABLE, false);
        cv.put(Tables.TrackPolicies.LAST_UPDATED, System.currentTimeMillis());

        insertInto(Tables.TrackPolicies.TABLE, cv.get());
    }

    public void insertPolicyHighTierMonetizable(Urn urn) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(Tables.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(Tables.TrackPolicies.POLICY, "SNIP");
        cv.put(Tables.TrackPolicies.MONETIZABLE, true);
        cv.put(Tables.TrackPolicies.MONETIZATION_MODEL, "SUB_HIGH_TIER");
        cv.put(Tables.TrackPolicies.SUB_MID_TIER, false);
        cv.put(Tables.TrackPolicies.SUB_HIGH_TIER, true);
        cv.put(Tables.TrackPolicies.SYNCABLE, false);

        insertInto(Tables.TrackPolicies.TABLE, cv.get());
    }

    public void insertTrackPendingDownload(Urn trackUrn, long requestedAt) {
        ContentValues cv = new ContentValues();
        cv.put(TrackDownloads._ID.name(), trackUrn.getNumericId());
        cv.put(TrackDownloads.REQUESTED_AT.name(), requestedAt);
        insertInto(TrackDownloads.TABLE, cv);
    }

    public void insertCompletedTrackDownload(Urn trackUrn, long requestedAtTimestamp, long completedTimestamp) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TrackDownloads.REQUESTED_AT, requestedAtTimestamp);
        cv.put(TrackDownloads.DOWNLOADED_AT, completedTimestamp);
        insertInto(TrackDownloads.TABLE, cv.get());
    }

    public void insertUnavailableTrackDownload(Urn trackUrn, long unavailableTimestamp) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TrackDownloads.REQUESTED_AT, unavailableTimestamp - 1);
        cv.put(TrackDownloads.UNAVAILABLE_AT, unavailableTimestamp);
        insertInto(TrackDownloads.TABLE, cv.get());
    }

    public void insertTrackDownloadPendingRemoval(Urn trackUrn, long removedAtTimestamp) {
        insertTrackDownloadPendingRemoval(trackUrn, 0, removedAtTimestamp);
    }

    public void insertTrackDownloadPendingRemoval(Urn trackUrn, long requestedAtTimestamp, long removedAtTimestamp) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(TrackDownloads._ID, trackUrn.getNumericId());
        cv.put(TrackDownloads.REQUESTED_AT, requestedAtTimestamp);
        cv.put(TrackDownloads.DOWNLOADED_AT, requestedAtTimestamp);
        cv.put(TrackDownloads.REMOVED_AT, removedAtTimestamp);
        insertInto(TrackDownloads.TABLE, cv.get());
    }

    public ApiPlaylist insertPlaylistMarkedForOfflineSync() {
        final ApiPlaylist apiPlaylist = insertPlaylist();
        insertPlaylistMarkedForOfflineSync(apiPlaylist);
        return apiPlaylist;
    }

    public void insertPlaylistMarkedForOfflineSync(ApiPlaylist playlist) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(OfflineContent._ID, playlist.getUrn().getNumericId());
        cv.put(OfflineContent._TYPE, OfflineContent.TYPE_PLAYLIST);
        insertInto(OfflineContent.TABLE, cv.get());
    }

    public void insertLikesMarkedForOfflineSync() {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(OfflineContent._ID, OfflineContent.ID_OFFLINE_LIKES);
        cv.put(OfflineContent._TYPE, OfflineContent.TYPE_COLLECTION);
        insertInto(OfflineContent.TABLE, cv.get());
    }

    public void insertUnsupportedActivity() {
        ContentValuesBuilder builder = ContentValuesBuilder.values();
        builder.put(TableColumns.Activities.TYPE, "unsupported type");
        builder.put(TableColumns.Activities.USER_ID, 123L);
        builder.put(TableColumns.Activities.CREATED_AT, new Date().getTime());
        insertInto(Table.Activities, builder.get());
    }

    public void insertUserFollowActivity(ApiUserFollowActivity followActivity) {
        insertUser(followActivity.getUser());
        ContentValuesBuilder builder = ContentValuesBuilder.values();
        builder.put(TableColumns.Activities.TYPE, ActivityKind.USER_FOLLOW.identifier());
        builder.put(TableColumns.Activities.USER_ID, followActivity.getUserUrn().getNumericId());
        builder.put(TableColumns.Activities.CREATED_AT, followActivity.getCreatedAt().getTime());
        insertInto(Table.Activities, builder.get());
    }

    public void insertTrackLikeActivity(ApiTrackLikeActivity activity) {
        insertTrack(activity.getTrack());
        insertUser(activity.getUser());
        ContentValuesBuilder builder = ContentValuesBuilder.values();
        builder.put(TableColumns.Activities.TYPE, ActivityKind.TRACK_LIKE.identifier());
        builder.put(TableColumns.Activities.USER_ID, activity.getUserUrn().getNumericId());
        builder.put(TableColumns.Activities.CREATED_AT, activity.getCreatedAt().getTime());
        builder.put(TableColumns.Activities.SOUND_ID, activity.getTrack().getId());
        builder.put(TableColumns.Activities.SOUND_TYPE, Tables.Sounds.TYPE_TRACK);
        insertInto(Table.Activities, builder.get());
    }

    public void insertTrackCommentActivity(ApiTrackCommentActivity activity) {
        insertTrack(activity.getTrack());
        insertComment(activity.getComment());
        ContentValuesBuilder builder = ContentValuesBuilder.values();
        builder.put(TableColumns.Activities.TYPE, ActivityKind.TRACK_COMMENT.identifier());
        builder.put(TableColumns.Activities.USER_ID, activity.getUserUrn().getNumericId());
        builder.put(TableColumns.Activities.CREATED_AT, activity.getCreatedAt().getTime());
        builder.put(TableColumns.Activities.SOUND_ID, activity.getTrack().getId());
        builder.put(TableColumns.Activities.SOUND_TYPE, Tables.Sounds.TYPE_TRACK);
        insertInto(Table.Activities, builder.get());
    }

    public void insertPlaylistRepostActivity(ApiPlaylistRepostActivity activity) {
        insertPlaylist(activity.getPlaylist());
        insertPlaylistRepostActivityWithoutPlaylist(activity);
    }

    public void insertPlaylistRepostActivityWithoutPlaylist(ApiPlaylistRepostActivity activity) {
        insertUser(activity.getUser());
        ContentValuesBuilder builder = ContentValuesBuilder.values();
        builder.put(TableColumns.Activities.TYPE, ActivityKind.TRACK_REPOST.identifier());
        builder.put(TableColumns.Activities.USER_ID, activity.getUserUrn().getNumericId());
        builder.put(TableColumns.Activities.CREATED_AT, activity.getCreatedAt().getTime());
        builder.put(TableColumns.Activities.SOUND_ID, activity.getPlaylist().getId());
        builder.put(TableColumns.Activities.SOUND_TYPE, Tables.Sounds.TYPE_PLAYLIST);
        insertInto(Table.Activities, builder.get());
    }

    public void insertComment(ApiComment comment) {
        insertUser(comment.getUser());
        ContentValuesBuilder builder = ContentValuesBuilder.values();
        builder.put(TableColumns.Comments.CREATED_AT, comment.getCreatedAt().getTime());
        builder.put(TableColumns.Comments.BODY, comment.getBody());
        builder.put(TableColumns.Comments.TIMESTAMP, comment.getTrackTime());
        builder.put(TableColumns.Comments.TRACK_ID, comment.getTrackUrn().getNumericId());
        builder.put(TableColumns.Comments.USER_ID, comment.getUser().getUrn().getNumericId());
    }

    public void insertSyncState(Uri contentUri,
                                long lastSyncAttempt,
                                long lastSyncSuccess,
                                String extra) {
        ContentValuesBuilder builder = ContentValuesBuilder.values();
        builder.put(TableColumns.Collections.URI, contentUri.toString());
        builder.put(TableColumns.Collections.LAST_SYNC_ATTEMPT, lastSyncAttempt);
        builder.put(TableColumns.Collections.LAST_SYNC, lastSyncSuccess);
        builder.put(TableColumns.Collections.EXTRA, extra);
        insertInto(Table.Collections, builder.get());
    }

    public long insertInto(com.soundcloud.propeller.schema.Table table, ContentValues cv) {
        final long rowId = database.insertWithOnConflict(table.name(), null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            throw new AssertionError("Failed inserting record into table " + table.name());
        }
        return rowId;
    }
}
