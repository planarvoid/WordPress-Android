package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PostProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PlaylistPostStorageTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);
    private static final Date POSTED_DATE_3 = new Date(300000);

    private PlaylistPostStorage storage;
    private ApiUser user;
    private PropertySet playlist1;
    private PropertySet playlist2;

    private TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        user = testFixtures().insertUser();

        storage = new PlaylistPostStorage(propellerRx());

        when(accountOperations.getLoggedInUserUrn()).thenReturn(user.getUrn());
    }

    @Test
    public void shouldLoadAllPlaylistPosts() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void shouldReturnTrackCountAsMaximumOfRemoteAndLocalCounts() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        assertThat(playlist1.get(PlaylistProperty.TRACK_COUNT)).isEqualTo(2);

        final Urn playlistUrn = playlist1.get(PlaylistProperty.URN);
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).subscribe(subscriber);

        final List<PropertySet> result = subscriber.getOnNextEvents().get(0);
        assertThat(result.get(1).get(PlaylistProperty.URN)).isEqualTo(playlistUrn);
        assertThat(result.get(1).get(PlaylistProperty.TRACK_COUNT)).isEqualTo(3);
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        storage.loadPostedPlaylists(1, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist2));
    }

    @Test
    public void shouldAdhereToPostedTime() throws Exception {
        playlist1 = createPlaylistRepostAt(POSTED_DATE_1, POSTED_DATE_1);
        playlist2 = createPlaylistRepostAt(POSTED_DATE_3, POSTED_DATE_1);

        storage.loadPostedPlaylists(2, POSTED_DATE_2.getTime()).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }

    @Test
    public void shouldIncludeLikeStatus() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        testFixtures().insertLike(new ApiLike(playlist1.get(PlaylistProperty.URN), new Date()));
        playlist1.put(PlayableProperty.IS_LIKED, true);

        storage.loadPostedPlaylists(1, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }

    @Test
    public void shouldNotIncludePlaylistsNotPresentInTheCollectionItemsTable() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        createPlaylistAt(POSTED_DATE_3); // deleted

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void shouldNotConfuseTracksForPlaylists() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        createTrackWithId(9999);
        createPlaylistCollectionWithId(9999, new Date());

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void loadRequestedDownloadStateWhenPlaylistIsMarkedForOfflineAndHasDownloadRequests() throws Exception {
        final ApiPlaylist postedPlaylist = insertPlaylistWithRequestedDownload(POSTED_DATE_1, System.currentTimeMillis());

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(createPostAndRequestedPropertySet(postedPlaylist)));
    }

    @Test
    public void loadDownloadedStateWhenPlaylistIsMarkedForOfflineAndNoDownloadRequest() throws Exception {
        final ApiPlaylist postedPlaylist = insertPlaylistWithDownloadedTrack(POSTED_DATE_1, 123L, System.currentTimeMillis());

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(createPostAndDownloadedPropertySet(postedPlaylist)));
    }

    @Test
    public void loadDownloadedStateOfTwoDifferentPlaylistsDoesNotInfluenceEachOther() throws Exception {
        final ApiPlaylist postedPlaylistDownloaded = insertPlaylistWithDownloadedTrack(POSTED_DATE_1, 123L, System.currentTimeMillis());
        final PropertySet downloadedPlaylist = createPostAndDownloadedPropertySet(postedPlaylistDownloaded);

        final ApiPlaylist postedPlaylistRequested = insertPlaylistWithRequestedDownload(POSTED_DATE_2, System.currentTimeMillis());
        final PropertySet requestedPlaylist = createPostAndRequestedPropertySet(postedPlaylistRequested);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(requestedPlaylist, downloadedPlaylist));
    }

    private PropertySet createPostAndRequestedPropertySet(ApiPlaylist postedPlaylistRequested) {
        return createPostPropertySet(postedPlaylistRequested)
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                .put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED);
    }

    private PropertySet createPostAndDownloadedPropertySet(ApiPlaylist postedPlaylistDownloaded) {
        return createPostPropertySet(postedPlaylistDownloaded)
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                .put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED);
    }

    private ApiPlaylist insertPlaylistWithRequestedDownload(Date postedDate, long requestedAt) {
        final ApiPlaylist postedPlaylist = createPlaylistAt(postedDate);
        final ApiTrack track = testFixtures().insertPlaylistTrack(postedPlaylist, 0);
        createPlaylistCollectionWithId(postedPlaylist.getUrn().getNumericId(), postedDate);
        testFixtures().insertPlaylistMarkedForOfflineSync(postedPlaylist);
        testFixtures().insertTrackPendingDownload(track.getUrn(), requestedAt);
        return postedPlaylist;
    }

    private ApiPlaylist insertPlaylistWithDownloadedTrack(Date postedDate, long requestedAt, long completedAt) {
        final ApiPlaylist postedPlaylistDownloaded = createPlaylistAt(postedDate);
        final ApiTrack trackDownloaded = testFixtures().insertPlaylistTrack(postedPlaylistDownloaded, 0);
        createPlaylistCollectionWithId(postedPlaylistDownloaded.getUrn().getNumericId(), postedDate);
        testFixtures().insertPlaylistMarkedForOfflineSync(postedPlaylistDownloaded);
        testFixtures().insertCompletedTrackDownload(trackDownloaded.getUrn(), requestedAt, completedAt);
        return postedPlaylistDownloaded;
    }

    private PropertySet createPlaylistPostAt(Date postedAt) {
        return createPlaylistRepostAt(postedAt, postedAt);
    }

    private PropertySet createPlaylistRepostAt(Date postedAt, Date createdAt) {
        ApiPlaylist playlist = createPlaylistAt(createdAt);
        createPlaylistCollectionWithId(playlist.getUrn().getNumericId(), postedAt);
        return createPostPropertySet(playlist).put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, false);
    }

    private PropertySet createPostPropertySet(ApiPlaylist playlist) {
        return playlist.toPropertySet().slice(
                PlaylistProperty.URN,
                PlaylistProperty.TITLE,
                PlaylistProperty.CREATOR_NAME,
                PlaylistProperty.TRACK_COUNT,
                PlaylistProperty.LIKES_COUNT,
                PlaylistProperty.IS_PRIVATE
        ).put(PostProperty.CREATED_AT, playlist.getCreatedAt())
                .put(PlayableProperty.IS_LIKED, false);
    }

    private ApiPlaylist createPlaylistAt(Date creationDate) {
        return testFixtures().insertPlaylistWithCreationDate(user, creationDate);
    }

    private void createPlaylistCollectionWithId(long playlistId, Date postedAt) {
        testFixtures().insertPlaylistPost(playlistId, postedAt.getTime(), false);
    }

    private void createTrackWithId(long trackId) {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setUser(user);
        apiTrack.setUrn(Urn.forTrack(trackId));
        testFixtures().insertTrack(apiTrack);
        testFixtures().insertTrackPost(apiTrack.getId(), apiTrack.getCreatedAt().getTime(), false);
    }
}
