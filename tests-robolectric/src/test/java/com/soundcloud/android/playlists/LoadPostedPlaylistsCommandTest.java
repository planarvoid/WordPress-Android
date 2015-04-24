package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.likes.ChronologicalQueryParams;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class LoadPostedPlaylistsCommandTest extends StorageIntegrationTest {

    private static final Date POSTED_DATE_1 = new Date(100000);
    private static final Date POSTED_DATE_2 = new Date(200000);
    private static final Date POSTED_DATE_3 = new Date(300000);

    private LoadPostedPlaylistsCommand command;
    private ApiUser user;
    private PropertySet playlist1;
    private PropertySet playlist2;

    @Mock private AccountOperations accountOperations;

    @Before
    public void setUp() throws Exception {
        user = testFixtures().insertUser();

        command = new LoadPostedPlaylistsCommand(propeller());

        when(accountOperations.getLoggedInUserUrn()).thenReturn(user.getUrn());
    }

    @Test
    public void shouldLoadAllPlaylistPosts() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toEqual(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void shouldReturnTrackCountAsMaximumOfRemoteAndLocalCounts() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        expect(playlist1.get(PlaylistProperty.TRACK_COUNT)).toEqual(2);

        final Urn playlistUrn = playlist1.get(PlaylistProperty.URN);
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result.get(1).get(PlaylistProperty.URN)).toEqual(playlistUrn);
        expect(result.get(1).get(PlaylistProperty.TRACK_COUNT)).toEqual(3);
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        List<PropertySet> result = command.with(new ChronologicalQueryParams(1, Long.MAX_VALUE)).call();

        expect(result).toEqual(Arrays.asList(playlist2));
    }

    @Test
    public void shouldAdhereToTimestamp() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        List<PropertySet> result = command.with(new ChronologicalQueryParams(2, POSTED_DATE_2.getTime())).call();

        expect(result).toEqual(Arrays.asList(playlist1));
    }

    @Test
    public void shouldIncludeLikeStatus() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        testFixtures().insertLike(new ApiLike(playlist1.get(PlaylistProperty.URN), new Date()));
        playlist1.put(PlayableProperty.IS_LIKED, true);

        List<PropertySet> result = command.with(new ChronologicalQueryParams(1, Long.MAX_VALUE)).call();

        expect(result).toEqual(Arrays.asList(playlist1));
    }

    @Test
    public void shouldNotIncludePlaylistsNotPresentInTheCollectionItemsTable() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        ApiPlaylist deletedPlaylist = createPlaylistAt(POSTED_DATE_3);
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).not.toContain(createPostPropertySet(deletedPlaylist));
        expect(result).toContain(playlist2, playlist1);
    }

    @Test
    public void shouldNotConfuseTracksForPlaylists() throws Exception {
        playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        createTrackWithId(9999);
        createPlaylistCollectionWithId(9999, new Date());
        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(playlist2, playlist1);
    }

    @Test
    public void loadRequestedDownloadStateWhenPlaylistIsMarkedForOfflineAndHasDownloadRequests() throws Exception {
        final ApiPlaylist postedPlaylist = insertPlaylistWithRequestedDownload(POSTED_DATE_1, System.currentTimeMillis());

        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();
        final PropertySet expected = createPostAndRequestedPropertySet(postedPlaylist);

        expect(result).toContainExactly(expected);
    }

    @Test
    public void loadDownloadedStateWhenPlaylistIsMarkedForOfflineAndNoDownloadRequest() throws Exception {
        final ApiPlaylist postedPlaylist = insertPlaylistWithDownloadedTrack(POSTED_DATE_1, 123L, System.currentTimeMillis());

        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();
        final PropertySet expected = createPostAndDownloadedPropertySet(postedPlaylist);

        expect(result).toContainExactly(expected);
    }

    @Test
    public void loadDownloadedStateOfTwoDifferentPlaylistsDoesNotInfluenceEachOther() throws Exception {
        final ApiPlaylist postedPlaylistDownloaded = insertPlaylistWithDownloadedTrack(POSTED_DATE_1, 123L, System.currentTimeMillis());
        final PropertySet downloadedPlaylist = createPostAndDownloadedPropertySet(postedPlaylistDownloaded);

        final ApiPlaylist postedPlaylistRequested = insertPlaylistWithRequestedDownload(POSTED_DATE_2, System.currentTimeMillis());
        final PropertySet requestedPlaylist = createPostAndRequestedPropertySet(postedPlaylistRequested);

        List<PropertySet> result = command.with(new ChronologicalQueryParams(10, Long.MAX_VALUE)).call();

        expect(result).toContainExactly(requestedPlaylist, downloadedPlaylist);
    }

    private PropertySet createPostAndRequestedPropertySet(ApiPlaylist postedPlaylistRequested) {
        return createPostPropertySet(postedPlaylistRequested)
                .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                .put(OfflineProperty.DOWNLOAD_STATE, DownloadState.REQUESTED);
    }

    private PropertySet createPostAndDownloadedPropertySet(ApiPlaylist postedPlaylistDownloaded) {
        return createPostPropertySet(postedPlaylistDownloaded)
                    .put(OfflineProperty.Collection.IS_MARKED_FOR_OFFLINE, true)
                    .put(OfflineProperty.DOWNLOAD_STATE, DownloadState.DOWNLOADED);
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
        ApiPlaylist playlist = createPlaylistAt(postedAt);
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
                PlaylistProperty.CREATED_AT,
                PlaylistProperty.IS_PRIVATE
        ).put(PlayableProperty.IS_LIKED, false);
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
        apiTrack.setId(trackId);
        testFixtures().insertTrack(apiTrack);
        testFixtures().insertTrackPost(apiTrack.getId(), apiTrack.getCreatedAt().getTime(), false);
    }
}
