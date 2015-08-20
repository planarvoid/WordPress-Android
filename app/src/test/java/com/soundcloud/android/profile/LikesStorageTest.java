package com.soundcloud.android.profile;

import static java.util.Arrays.asList;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.likes.LikeProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Date;
import java.util.List;

public class LikesStorageTest extends StorageIntegrationTest {

    public static final Date LIKED_AT_1 = new Date(1000);
    public static final Date LIKED_AT_2 = new Date(2000);
    public static final Date LIKED_AT_3 = new Date(3000);
    public static final Date LIKED_AT_4 = new Date(4000);

    private LikesStorage storage;

    private TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();
    private TestSubscriber<List<Urn>> playbackSubscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        storage = new LikesStorage(propellerRx());
    }

    @Test
    public void loadsAllLikes() {
        PropertySet trackLike1 = createTrackLikeAt(LIKED_AT_1);
        PropertySet playlistLike1 = createPlaylistLikeAt(LIKED_AT_2);

        storage.loadLikes(2, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(asList(playlistLike1, trackLike1));
    }

    @Test
    public void loadsLikesAdheringToLimit() throws Exception {
        createTrackLikeAt(LIKED_AT_1);
        PropertySet trackLike2 = createTrackLikeAt(LIKED_AT_2);

        storage.loadLikes(1, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(asList(trackLike2));
    }

    @Test
    public void loadsLikesAdheringTimestamp() throws Exception {
        PropertySet trackLike1 = createTrackLikeAt(LIKED_AT_1);
        createTrackLikeAt(LIKED_AT_2);

        storage.loadLikes(1, LIKED_AT_2.getTime()).subscribe(subscriber);

        subscriber.assertValues(asList(trackLike1));
    }

    @Test
    public void loadsLikesWithPendingAdditions() {
        PropertySet trackLike1 = createTrackLikeAt(LIKED_AT_1);
        PropertySet trackLike2 = createTrackLikePendingAdditionAt(LIKED_AT_2);
        PropertySet playlistLike1 = createPlaylistLikeAt(LIKED_AT_3);
        PropertySet playlistLike2 = createPlaylistLikePendingAdditionAt(LIKED_AT_4);

        storage.loadLikes(4, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(asList(
                playlistLike2,
                playlistLike1,
                trackLike2,
                trackLike1));
    }

    @Test
    public void loadsLikesWithoutPendingRemovals() {
        PropertySet trackLike1 = createTrackLikeAt(LIKED_AT_1);
        createTrackLikePendingRemovalAt(LIKED_AT_2, new Date());
        PropertySet playlistLike1 = createPlaylistLikeAt(LIKED_AT_3);
        createPlaylistLikePendingRemovalAt(LIKED_AT_4, new Date());

        storage.loadLikes(2, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValues(asList(playlistLike1, trackLike1));
    }

    @Test
    public void loadsTrackLikesForPlayback() {
        PropertySet trackLike1 = createTrackLikeAt(LIKED_AT_1);
        PropertySet trackLike2 = createTrackLikeAt(LIKED_AT_2);
        createPlaylistLikeAt(LIKED_AT_2);

        storage.loadLikesForPlayback().subscribe(playbackSubscriber);

        playbackSubscriber.assertValues(
                asList(
                        trackLike2.get(TrackProperty.URN),
                        trackLike1.get(TrackProperty.URN)
                )
        );
    }

    @Test
    public void loadsTrackLikesForPlaybackWithoutPendingAdditions() {
        PropertySet trackLike1 = createTrackLikeAt(LIKED_AT_1);
        PropertySet trackLike2 = createTrackLikePendingAdditionAt(LIKED_AT_2);
        createPlaylistLikeAt(LIKED_AT_2);

        storage.loadLikesForPlayback().subscribe(playbackSubscriber);

        playbackSubscriber.assertValues(
                asList(
                        trackLike2.get(TrackProperty.URN),
                        trackLike1.get(TrackProperty.URN)
                )
        );
    }

    @Test
    public void loadsTrackLikesForPlaybackWithoutPendingRemovals() {
        PropertySet trackLike1 = createTrackLikeAt(LIKED_AT_1);
        createTrackLikePendingRemovalAt(LIKED_AT_2, new Date());
        createPlaylistLikeAt(LIKED_AT_2);

        storage.loadLikesForPlayback().subscribe(playbackSubscriber);

        playbackSubscriber.assertValues(asList(trackLike1.get(TrackProperty.URN)));
    }

    private PropertySet createTrackLikeAt(Date likedAt) {
        final ApiTrack apiTrack = testFixtures().insertLikedTrack(likedAt);
        return createLikedPropertySet(likedAt, apiTrack);
    }

    private PropertySet createTrackLikePendingAdditionAt(Date likedAt) {
        final ApiTrack apiTrack = testFixtures().insertLikedTrackPendingAddition(likedAt);
        return createLikedPropertySet(likedAt, apiTrack);
    }

    private PropertySet createTrackLikePendingRemovalAt(Date likedAt, Date unlikedDate) {
        final ApiTrack apiTrack = testFixtures().insertLikedTrackPendingRemoval(likedAt, unlikedDate);
        return createLikedPropertySet(likedAt, apiTrack);
    }

    private PropertySet createLikedPropertySet(Date likedAt, ApiTrack apiTrack) {
        return apiTrack.toPropertySet().slice(
                TrackProperty.URN,
                TrackProperty.TITLE,
                TrackProperty.CREATOR_NAME,
                TrackProperty.LIKES_COUNT,
                TrackProperty.DURATION,
                TrackProperty.IS_PRIVATE
        ).put(LikeProperty.CREATED_AT, likedAt)
                .put(PlayableProperty.IS_LIKED, true);
    }

    private PropertySet createPlaylistLikeAt(Date likedAt) {
        final ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylist(likedAt);
        return createLikedPropertySet(likedAt, apiPlaylist);
    }

    private PropertySet createPlaylistLikePendingAdditionAt(Date likedAt) {
        final ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylistPendingAddition(likedAt);
        return createLikedPropertySet(likedAt, apiPlaylist);
    }

    private PropertySet createPlaylistLikePendingRemovalAt(Date likedAt, Date unlikedDate) {
        final ApiPlaylist apiPlaylist = testFixtures().insertLikedPlaylistPendingRemoval(likedAt, unlikedDate);
        return createLikedPropertySet(likedAt, apiPlaylist);
    }

    private PropertySet createLikedPropertySet(Date likedAt, ApiPlaylist apiPlaylist) {
        return apiPlaylist.toPropertySet().slice(
                PlaylistProperty.URN,
                PlaylistProperty.TITLE,
                PlaylistProperty.CREATOR_NAME,
                PlaylistProperty.LIKES_COUNT,
                PlaylistProperty.TRACK_COUNT,
                PlaylistProperty.IS_PRIVATE
        ).put(LikeProperty.CREATED_AT, likedAt)
                .put(PlayableProperty.IS_LIKED, true);
    }
}