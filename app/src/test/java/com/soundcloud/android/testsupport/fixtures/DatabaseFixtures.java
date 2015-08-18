package com.soundcloud.android.testsupport.fixtures;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.model.stream.ApiStreamItem;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.stations.ApiStation;
import com.soundcloud.android.stations.StationFixtures;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.storage.Tables.OfflineContent;
import com.soundcloud.android.storage.Tables.RecentStations;
import com.soundcloud.android.storage.Tables.Stations;
import com.soundcloud.android.storage.Tables.StationsPlayQueues;
import com.soundcloud.android.storage.Tables.TrackDownloads;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.sync.posts.ApiPost;
import com.soundcloud.android.tracks.TrackRecord;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.propeller.ContentValuesBuilder;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import java.util.Date;
import java.util.List;

public class DatabaseFixtures {

    private final SQLiteDatabase database;

    public DatabaseFixtures(SQLiteDatabase database) {
        this.database = database;
    }

    public ApiTrack insertTrack() {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertUser(track.getUser());
        insertTrack(track);
        return track;
    }

    public void insertTrack(ApiTrack track) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, track.getUrn().getNumericId());
        cv.put(TableColumns.Sounds.TITLE, track.getTitle());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Sounds.USER_ID, track.getUser().getUrn().getNumericId());
        cv.put(TableColumns.Sounds.DURATION, track.getDuration());
        cv.put(TableColumns.Sounds.WAVEFORM_URL, track.getWaveformUrl());
        cv.put(TableColumns.Sounds.STREAM_URL, track.getStreamUrl());
        cv.put(TableColumns.Sounds.LIKES_COUNT, track.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.REPOSTS_COUNT, track.getStats().getRepostsCount());
        cv.put(TableColumns.Sounds.PLAYBACK_COUNT, track.getStats().getPlaybackCount());
        cv.put(TableColumns.Sounds.COMMENT_COUNT, track.getStats().getCommentsCount());
        cv.put(TableColumns.Sounds.PERMALINK_URL, track.getPermalinkUrl());
        cv.put(TableColumns.Sounds.SHARING, track.getSharing().value());
        cv.put(TableColumns.Sounds.CREATED_AT, track.getCreatedAt().getTime());

        insertInto(Table.Sounds, cv);
        insertPolicy(track);
    }

    public void insertFollowing(Urn followedUrn) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.UserAssociations.ASSOCIATION_TYPE, TableColumns.UserAssociations.TYPE_FOLLOWING);
        cv.put(TableColumns.UserAssociations.TARGET_ID, followedUrn.getNumericId());
        cv.put(TableColumns.UserAssociations.CREATED_AT, System.currentTimeMillis());
        insertInto(Table.UserAssociations, cv);
    }

    public void insertFollowingPendingRemoval(Urn followedUrn) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.UserAssociations.ASSOCIATION_TYPE, TableColumns.UserAssociations.TYPE_FOLLOWING);
        cv.put(TableColumns.UserAssociations.TARGET_ID, followedUrn.getNumericId());
        cv.put(TableColumns.UserAssociations.CREATED_AT, System.currentTimeMillis());
        cv.put(TableColumns.UserAssociations.REMOVED_AT, System.currentTimeMillis());
        insertInto(Table.UserAssociations, cv);
    }

    public void insertRecentlyPlayedStation(Urn stationUrn, int startedAt) {
        ContentValuesBuilder cv = ContentValuesBuilder.values();
        cv.put(RecentStations.STATION_URN, stationUrn.toString());
        cv.put(RecentStations.STARTED_AT, startedAt);
        insertInto(RecentStations.TABLE, cv.get());
    }

    private void insertPolicy(ApiTrack track) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackPolicies.TRACK_ID, track.getId());
        cv.put(TableColumns.TrackPolicies.POLICY, track.getPolicy());
        cv.put(TableColumns.TrackPolicies.MONETIZABLE, track.isMonetizable());
        cv.put(TableColumns.TrackPolicies.SYNCABLE, track.isSyncable());
        cv.put(TableColumns.TrackPolicies.LAST_UPDATED, System.currentTimeMillis());

        insertInto(Table.TrackPolicies, cv);
    }

    public void insertDescription(Urn trackUrn, String description) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, trackUrn.getNumericId());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Sounds.DESCRIPTION, description);
        insertInto(Table.Sounds, cv);
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

    public ApiPlaylist insertLocalPlaylist() {
        return insertLocalPlaylist(ModelFixtures.create(ApiPlaylist.class));
    }

    public ApiPlaylist insertLocalPlaylist(ApiPlaylist playlist) {
        playlist.setUrn(Urn.forPlaylist(-(1000 + playlist.getId())));
        insertUser(playlist.getUser());
        insertPlaylist(playlist);
        return playlist;
    }

    public void insertPlaylist(ApiPlaylist playlist) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Sounds._ID, playlist.getId());
        cv.put(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Sounds.TITLE, playlist.getTitle());
        cv.put(TableColumns.Sounds.USER_ID, playlist.getUser().getId());
        cv.put(TableColumns.Sounds.LIKES_COUNT, playlist.getStats().getLikesCount());
        cv.put(TableColumns.Sounds.REPOSTS_COUNT, playlist.getStats().getRepostsCount());
        cv.put(TableColumns.Sounds.PERMALINK_URL, playlist.getPermalinkUrl());
        cv.put(TableColumns.Sounds.SHARING, playlist.getSharing().value());
        cv.put(TableColumns.Sounds.DURATION, playlist.getDuration());
        cv.put(TableColumns.Sounds.TRACK_COUNT, playlist.getTrackCount());
        cv.put(TableColumns.Sounds.CREATED_AT, playlist.getCreatedAt().getTime());

        insertInto(Table.Sounds, cv);
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

    public ApiTrack insertTrackWithCreationDate(ApiUser user, Date createdAtDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        track.setCreatedAt(createdAtDate);
        track.setUser(user);
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
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Users._ID, user.getUrn().getNumericId());
        cv.put(TableColumns.Users.USERNAME, user.getUsername());
        cv.put(TableColumns.Users.COUNTRY, user.getCountry());
        cv.put(TableColumns.Users.FOLLOWERS_COUNT, user.getFollowersCount());

        insertInto(Table.Users, cv);
    }

    public void insertExtendedUser(ApiUser user, String description, String websiteUrl, String websiteTitle,
                                   String discogsName, String myspaceName) {

        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Users._ID, user.getUrn().getNumericId());
        cv.put(TableColumns.Users.USERNAME, user.getUsername());
        cv.put(TableColumns.Users.COUNTRY, user.getCountry());
        cv.put(TableColumns.Users.FOLLOWERS_COUNT, user.getFollowersCount());
        cv.put(TableColumns.Users.DESCRIPTION, description);
        cv.put(TableColumns.Users.WEBSITE_URL, websiteUrl);
        cv.put(TableColumns.Users.WEBSITE_NAME, websiteTitle);
        cv.put(TableColumns.Users.DISCOGS_NAME, discogsName);
        cv.put(TableColumns.Users.MYSPACE_NAME, myspaceName);

        insertInto(Table.Users, cv);
    }

    public void insertLike(long id, int type, Date createdAt) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Likes._ID, id);
        cv.put(TableColumns.Likes._TYPE, type);
        cv.put(TableColumns.Likes.CREATED_AT, createdAt.getTime());
        insertInto(Table.Likes, cv);
    }

    public ApiStation insertStation(int lastPlayedPosition) {
        final ApiStation station = StationFixtures.getApiStation();

        insertInto(Stations.TABLE, getStationContentValues(station, lastPlayedPosition));

        final List<? extends TrackRecord> playQueue = station.getTracks().getCollection();

        for (int i = 0; i < playQueue.size(); i++) {
            final TrackRecord track = playQueue.get(i);
            insertInto(StationsPlayQueues.TABLE, getTrackContentValues(i, station, track));
        }

        return station;
    }

    private ContentValues getStationContentValues(ApiStation station, int lastPlayedPosition) {
        final ContentValuesBuilder stationContentValues = ContentValuesBuilder.values();

        stationContentValues.put(Stations.STATION_URN, station.getUrn().toString());
        stationContentValues.put(Stations.TITLE, station.getTitle());
        stationContentValues.put(Stations.TYPE, station.getType());
        stationContentValues.put(Stations.PERMALINK, station.getPermalink());
        stationContentValues.put(Stations.LAST_PLAYED_TRACK_POSITION, lastPlayedPosition);

        return stationContentValues.get();
    }

    private ContentValues getTrackContentValues(int position, ApiStation stationInfo, TrackRecord track) {
        final ContentValuesBuilder trackContentValues = ContentValuesBuilder.values();
        
        trackContentValues.put(StationsPlayQueues.POSITION, position);
        trackContentValues.put(StationsPlayQueues.STATION_URN, stationInfo.getUrn().toString());
        trackContentValues.put(StationsPlayQueues.TRACK_URN, track.getUrn().toString());

        return trackContentValues.get();
    }

    public void insertLike(ApiLike like) {
        insertLike(
                like.getTargetUrn().getNumericId(),
                like.getTargetUrn().isTrack() ? TableColumns.Sounds.TYPE_TRACK : TableColumns.Sounds.TYPE_PLAYLIST,
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
        insertLike(track.getId(), TableColumns.Sounds.TYPE_TRACK, likedDate);
        return track;
    }

    public ApiTrack insertLikedTrackPendingRemoval(Date unlikedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertTrack(track);
        insertLike(track.getId(), TableColumns.Sounds.TYPE_TRACK, new Date(0));
        database.execSQL("UPDATE Likes SET removed_at=" + unlikedDate.getTime()
                + " WHERE _id=" + track.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_TRACK);
        return track;
    }

    public ApiTrack insertLikedTrackPendingAddition(Date likedDate) {
        ApiTrack track = ModelFixtures.create(ApiTrack.class);
        insertTrack(track);
        insertLike(track.getId(), TableColumns.Sounds.TYPE_TRACK, new Date(0));
        database.execSQL("UPDATE Likes SET added_at=" + likedDate.getTime()
                + " WHERE _id=" + track.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_TRACK);
        return track;
    }

    public ApiPlaylist insertLikedPlaylist(Date likedDate) {
        ApiPlaylist playlist = insertPlaylist();
        insertPlaylist(playlist);
        insertLike(playlist.getId(), TableColumns.Sounds.TYPE_PLAYLIST, likedDate);
        return playlist;
    }

    public ApiPlaylist insertLikedPlaylistPendingRemoval(Date unlikedDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertPlaylist(playlist);
        insertLike(playlist.getId(), TableColumns.Sounds.TYPE_PLAYLIST, new Date(0));
        database.execSQL("UPDATE Likes SET removed_at=" + unlikedDate.getTime()
                + " WHERE _id=" + playlist.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_PLAYLIST);
        return playlist;
    }

    public ApiPlaylist insertLikedPlaylistPendingAddition(Date likedDate) {
        ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        insertPlaylist(playlist);
        insertLike(playlist.getId(), TableColumns.Sounds.TYPE_PLAYLIST, new Date(0));
        database.execSQL("UPDATE Likes SET added_at=" + likedDate.getTime()
                + " WHERE _id=" + playlist.getUrn().getNumericId()
                + " AND _type=" + TableColumns.Sounds.TYPE_PLAYLIST);
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
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Posts.TARGET_ID, id);
        cv.put(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.Posts.TYPE, isRepost ? TableColumns.Posts.TYPE_REPOST : TableColumns.Posts.TYPE_POST);
        cv.put(TableColumns.Posts.CREATED_AT, createdAt);
        insertInto(Table.Posts, cv);
    }

    public void insertPlaylistRepost(long id, long createdAt) {
        insertPlaylistPost(id, createdAt, true);
    }

    public void insertPlaylistPost(long playlistId, long createdAt, boolean isRepost) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.Posts.TARGET_ID, playlistId);
        cv.put(TableColumns.Posts.TARGET_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.Posts.TYPE, isRepost ? TableColumns.Posts.TYPE_REPOST : TableColumns.Posts.TYPE_POST);
        cv.put(TableColumns.Posts.CREATED_AT, createdAt);
        insertInto(Table.Posts, cv);
    }

    public void insertStreamTrackPost(ApiStreamItem apiStreamItem) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, apiStreamItem.getTrack().get().getId());
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, apiStreamItem.getTrack().get().getCreatedAt().getTime());
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamTrackPost(long trackId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamTrackRepost(long trackId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, trackId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamPlaylistPost(long playlistId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        insertInto(Table.SoundStream, cv);
    }

    public void insertStreamPlaylistRepost(long playlistId, long timestamp, long reposterId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, playlistId);
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_PLAYLIST);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.REPOSTER_ID, reposterId);
        insertInto(Table.SoundStream, cv);
    }

    public ApiTrack insertPromotedStreamTrack(long timestamp) {
        return insertPromotedStreamTrack(insertTrack(), timestamp);
    }

    public ApiTrack insertPromotedStreamTrack(ApiTrack apiTrack, long timestasmp) {
        return insertPromotedStreamTrack(apiTrack, timestasmp, 26 /** why 26? - JS**/);
    }

    public ApiTrack insertPromotedStreamTrack(ApiTrack track, long timestamp, long promotedId) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.SoundStream.SOUND_ID, track.getUrn().getNumericId());
        cv.put(TableColumns.SoundStream.SOUND_TYPE, TableColumns.Sounds.TYPE_TRACK);
        cv.put(TableColumns.SoundStream.CREATED_AT, timestamp);
        cv.put(TableColumns.SoundStream.PROMOTED_ID, promotedId);
        insertInto(Table.SoundStream, cv);

        insertPromotedTrackMetadata(promotedId, timestamp);
        return track;
    }

    public void insertPromotedTrackMetadata(long promotedId, long timestamp) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.PromotedTracks._ID, promotedId);
        cv.put(TableColumns.PromotedTracks.CREATED_AT, timestamp);
        cv.put(TableColumns.PromotedTracks.AD_URN, "promoted:track:123");
        cv.put(TableColumns.PromotedTracks.PROMOTER_ID, 83);
        cv.put(TableColumns.PromotedTracks.PROMOTER_NAME, "SoundCloud");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_CLICKED_URLS, "promoted1 promoted2");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_IMPRESSION_URLS, "promoted3 promoted4");
        cv.put(TableColumns.PromotedTracks.TRACKING_TRACK_PLAYED_URLS, "promoted5 promoted6");
        cv.put(TableColumns.PromotedTracks.TRACKING_PROMOTER_CLICKED_URLS, "promoted7 promoted8");

        insertInto(Table.PromotedTracks, cv);
    }

    public void insertPolicyAllow(Urn urn, long lastUpdate) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(TableColumns.TrackPolicies.POLICY, "ALLOW");
        cv.put(TableColumns.TrackPolicies.MONETIZABLE, true);
        cv.put(TableColumns.TrackPolicies.SYNCABLE, true);
        cv.put(TableColumns.TrackPolicies.LAST_UPDATED, lastUpdate);

        insertInto(Table.TrackPolicies, cv);
    }

    public void insertPolicyBlock(Urn urn) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(TableColumns.TrackPolicies.POLICY, "BLOCK");
        cv.put(TableColumns.TrackPolicies.MONETIZABLE, false);
        cv.put(TableColumns.TrackPolicies.SYNCABLE, false);

        insertInto(Table.TrackPolicies, cv);
    }

    public void insertPolicyMidTierMonetizable(Urn urn) {
        ContentValues cv = new ContentValues();
        cv.put(TableColumns.TrackPolicies.TRACK_ID, urn.getNumericId());
        cv.put(TableColumns.TrackPolicies.POLICY, "SNIP");
        cv.put(TableColumns.TrackPolicies.MONETIZABLE, true);
        cv.put(TableColumns.TrackPolicies.MONETIZATION_MODEL, "SUB_MID_TIER");
        cv.put(TableColumns.TrackPolicies.SUB_MID_TIER, true);
        cv.put(TableColumns.TrackPolicies.SUB_HIGH_TIER, false);
        cv.put(TableColumns.TrackPolicies.SYNCABLE, false);

        insertInto(Table.TrackPolicies, cv);
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
        cv.put(TrackDownloads.REQUESTED_AT, 33333333L);
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

    public long insertInto(com.soundcloud.propeller.schema.Table table, ContentValues cv) {
        final long rowId = database.insertWithOnConflict(table.name(), null, cv, SQLiteDatabase.CONFLICT_REPLACE);
        if (rowId == -1) {
            throw new AssertionError("Failed inserting record into table " + table.name());
        }
        return rowId;
    }
}
