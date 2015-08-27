package com.soundcloud.android.likes;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static junit.framework.Assert.fail;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.playlists.PlaylistProperty;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class PlaylistLikesStorageTest extends StorageIntegrationTest {

    private static final Date LIKED_DATE_1 = new Date(100);
    private static final Date LIKED_DATE_2 = new Date(200);

    private PlaylistLikesStorage playlistLikesStorage;

    private PropertySet playlist1PropertySet;
    private PropertySet playlist2PropertySet;
    private ApiPlaylist playlist1;
    private TestSubscriber<List<PropertySet>> testListSubscriber;
    private TestSubscriber<PropertySet> testSubscriber;


    @Before
    public void setUp() throws Exception {
        testListSubscriber = new TestSubscriber<>();
        testSubscriber =  new TestSubscriber<>();
        playlistLikesStorage = new PlaylistLikesStorage(propellerRx());

        playlist1 = testFixtures().insertLikedPlaylist(LIKED_DATE_1);
        playlist1PropertySet = playlist1.toPropertySet();
        playlist2PropertySet = testFixtures().insertLikedPlaylist(LIKED_DATE_2).toPropertySet();
    }

    @Test
    public void loadAllLikedPlaylists() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PropertySet> propertySets = newArrayList(
                expectedLikedPlaylistFor(playlist2PropertySet, LIKED_DATE_2),
                expectedLikedPlaylistFor(playlist1PropertySet, LIKED_DATE_1));

        assertThat(testListSubscriber.getOnNextEvents()).containsExactly(propertySets);
    }

    @Test
    public void loadLikedPlaylistsAdhereToLimit() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PropertySet> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist2PropertySet, LIKED_DATE_2));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadLikedPlaylistsAdhereToTimestamp() throws Exception {
        playlistLikesStorage.loadLikedPlaylists(1, LIKED_DATE_2.getTime()).subscribe(testListSubscriber);

        final List<PropertySet> propertySets = Arrays.asList(
                expectedLikedPlaylistFor(playlist1PropertySet, LIKED_DATE_1));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadsRequestedDownloadStateForPlaylistMarkedForOffline() {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertTrackPendingDownload(apiTrack.getUrn(), LIKED_DATE_1.getTime());

        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PropertySet> propertySets = newArrayList(
                expectedLikedPlaylistFor(playlist2PropertySet, LIKED_DATE_2),
                expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.REQUESTED));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadsDownloadedStateForPlaylistMarkedForOffline() {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertCompletedTrackDownload(apiTrack.getUrn(), LIKED_DATE_1.getTime(), LIKED_DATE_1.getTime());

        playlistLikesStorage.loadLikedPlaylists(2, Long.MAX_VALUE).subscribe(testListSubscriber);

        final List<PropertySet> propertySets = newArrayList(
                expectedLikedPlaylistFor(playlist2PropertySet, LIKED_DATE_2),
                expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.DOWNLOADED));

        testListSubscriber.assertValue(propertySets);
    }

    @Test
    public void loadsDownloadedStateForPlaylistsWithOnlyCreatorOptOutTracks() {
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertUnavailableTrackDownload(apiTrack.getUrn(), 1000L);

        playlistLikesStorage.loadLikedPlaylist(playlist1.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertValue(expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.UNAVAILABLE));
    }

    @Test
    public void loadsDownloadedStateForPlaylistsWithSomeCreatorOptOutTracks() {
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertPlaylistMarkedForOfflineSync(playlist1);
        testFixtures().insertUnavailableTrackDownload(apiTrack1.getUrn(), 1000L);

        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(playlist1, 0);
        testFixtures().insertCompletedTrackDownload(apiTrack2.getUrn(), 1000L, 1100L);

        playlistLikesStorage.loadLikedPlaylist(playlist1.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertValue(expectedLikedPlaylistWithOfflineState(playlist1, LIKED_DATE_1, OfflineState.DOWNLOADED));
    }

    @Test
    public void loadLikedPlaylistShouldEmitEmptyPropertySetIfLikeDoesNotExist() {
        ApiPlaylist playlist = testFixtures().insertPlaylist();

        playlistLikesStorage.loadLikedPlaylist(playlist.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertValue(PropertySet.create());
    }

    @Test
    public void loadsPlaylistLike() throws Exception {
        PropertySet playlist = testFixtures().insertLikedPlaylist(LIKED_DATE_1).toPropertySet();

        playlistLikesStorage.loadLikedPlaylist(playlist.get(TrackProperty.URN)).subscribe(testSubscriber);

        testSubscriber.assertValue(expectedLikedPlaylistFor(playlist, LIKED_DATE_1));
    }

    @Test
    public void loadsPlaylistLikeWithTrackCountAsMaximumOfLocalAndRemoteFromDatabase() {
        ApiPlaylist playlist = testFixtures().insertLikedPlaylist(LIKED_DATE_1);

        assertThat(playlist.getTrackCount()).isEqualTo(2);

        final Urn playlistUrn = playlist.getUrn();
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        playlistLikesStorage.loadLikedPlaylist(playlistUrn).subscribe(testSubscriber);

        assertThat(testSubscriber.getOnNextEvents().get(0).get(PlaylistProperty.URN)).isEqualTo(playlistUrn);
        assertThat(testSubscriber.getOnNextEvents().get(0).get(PlaylistProperty.TRACK_COUNT)).isEqualTo(3);
    }

    static PropertySet expectedLikedPlaylistFor(PropertySet playlist, Date likedAt) {
        return PropertySet.from(
                PlaylistProperty.URN.bind(playlist.get(PlaylistProperty.URN)),
                PlaylistProperty.TITLE.bind(playlist.get(PlaylistProperty.TITLE)),
                PlaylistProperty.CREATOR_NAME.bind(playlist.get(PlaylistProperty.CREATOR_NAME)),
                PlaylistProperty.TRACK_COUNT.bind(playlist.get(PlaylistProperty.TRACK_COUNT)),
                PlaylistProperty.LIKES_COUNT.bind(playlist.get(PlaylistProperty.LIKES_COUNT)),
                LikeProperty.CREATED_AT.bind((likedAt)),
                PlaylistProperty.IS_PRIVATE.bind(playlist.get(PlaylistProperty.IS_PRIVATE)),
                PlaylistProperty.IS_LIKED.bind(true),
                OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE.bind(false));
    }

    private PropertySet expectedLikedPlaylistWithOfflineState(ApiPlaylist playlist, Date likedAt, OfflineState state) {
        return expectedLikedPlaylistFor(playlist.toPropertySet(), likedAt)
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                .put(OfflineProperty.OFFLINE_STATE, state);
    }
}