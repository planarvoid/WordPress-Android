package com.soundcloud.android.playlists;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestObserver;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTracksStorageTest extends StorageIntegrationTest {

    @Mock private DateProvider dateProvider;
    @Mock private AccountOperations accountOperations;

    private static final Date ADDED_AT = new Date();
    private PlaylistTracksStorage playlistTracksStorage;

    @Before
    public void setUp() throws Exception {
        playlistTracksStorage = new PlaylistTracksStorage(propellerRx(), dateProvider, accountOperations);

        when(dateProvider.getCurrentDate()).thenReturn(ADDED_AT);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(321L));
    }

    @Test
    public void playlistForAddingTrackLoadsPlaylistWithCorrectIsAddedStatus() {
        final TestObserver<List<PropertySet>> testObserver = new TestObserver<>();
        final ApiPlaylist apiPlaylist1 = insertPostedPlaylist();
        final ApiPlaylist apiPlaylist2 = insertPostedPlaylist();
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(apiPlaylist2.getUrn(), 0);

        playlistTracksStorage.playlistsForAddingTrack(apiTrack.getUrn())
                .subscribe(testObserver);

        List<PropertySet> result = testObserver.getOnNextEvents().get(0);
        expect(result).toContainExactly(
                playlistForTrackPropertySet(apiPlaylist1, false),
                playlistForTrackPropertySet(apiPlaylist2, true));
    }

    @Test
    public void playlistForAddingTracksDoNoIncludeRepostedPlaylists() {
        final TestObserver<List<PropertySet>> testObserver = new TestObserver<>();
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertPlaylistRepost(apiPlaylist.getId(), dateProvider.getCurrentDate().getTime());

        playlistTracksStorage.playlistsForAddingTrack(apiTrack.getUrn()).subscribe(testObserver);

        List<PropertySet> result = testObserver.getOnNextEvents().get(0);
        expect(result).toBeEmpty();
    }

    @Test
    public void insertsNewPlaylist() {
        final TestObserver<Urn> testObserver = new TestObserver<>();

        playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))
                .subscribe(testObserver);

        long playlistId = testObserver.getOnNextEvents().get(0).getNumericId();
        assertPlaylistInserted(playlistId, "title", true);
    }

    @Test
    public void insertsPlaylistPost() {
        final TestObserver<Urn> testObserver = new TestObserver<>();

        playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))
                .subscribe(testObserver);

        Urn playlistUrn = testObserver.getOnNextEvents().get(0);
        databaseAssertions().assertPlaylistPostInsertedFor(playlistUrn);
    }

    @Test
    public void insertsFirstPlaylistTrack() {
        final TestObserver<Urn> testObserver = new TestObserver<>();

        playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))
                .subscribe(testObserver);

        long playlistId = testObserver.getOnNextEvents().get(0).getNumericId();
        databaseAssertions().assertPlaylistTracklist(playlistId, Arrays.asList(Urn.forTrack(123)));
    }

    @Test
    public void returnsPlaylistTracks() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);

        testFixtures().insertPlaylistTrack(testFixtures().insertPlaylist(), 2);

        final List<PropertySet> tracks = playlistTracksStorage.playlistTracks(apiPlaylist.getUrn()).toBlocking().single();

        expect(tracks).toContainExactly(
                fromApiTrack(apiTrack1),
                fromApiTrack(apiTrack2)
        );
    }

    @Test
    public void returnsPlaylistTracksWithOfflineInfo() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        testFixtures().insertCompletedTrackDownload(apiTrack1.getUrn(), 100L);
        testFixtures().insertTrackPendingDownload(apiTrack2.getUrn(), 200L);
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack3.getUrn(), 300L);

        final List<PropertySet> tracks = playlistTracksStorage.playlistTracks(apiPlaylist.getUrn()).toBlocking().single();

        expect(tracks).toContain(
                fromApiTrack(apiTrack1)
                        .put(OfflineProperty.DOWNLOADED_AT, new Date(100L))
                        .put(OfflineProperty.REQUESTED_AT, new Date(100L)),
                fromApiTrack(apiTrack2)
                        .put(OfflineProperty.REQUESTED_AT, new Date(200L)),
                fromApiTrack(apiTrack3)
                        .put(OfflineProperty.REMOVED_AT, new Date(300L))
                        .put(OfflineProperty.REQUESTED_AT, new Date(300L))
        );
    }

    private PropertySet fromApiTrack(ApiTrack apiTrack){
        return PropertySet.from(
                TrackProperty.URN.bind(apiTrack.getUrn()),
                TrackProperty.TITLE.bind(apiTrack.getTitle()),
                TrackProperty.DURATION.bind(apiTrack.getDuration()),
                TrackProperty.PLAY_COUNT.bind(apiTrack.getStats().getPlaybackCount()),
                TrackProperty.LIKES_COUNT.bind(apiTrack.getStats().getLikesCount()),
                TrackProperty.IS_PRIVATE.bind(apiTrack.isPrivate()),
                TrackProperty.CREATOR_NAME.bind(apiTrack.getUserName()),
                TrackProperty.CREATOR_URN.bind(apiTrack.getUser().getUrn())
        );
    }

    private void assertPlaylistInserted(long playlistId, String title, boolean isPrivate) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlistId)
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Sounds.USER_ID, 321L)
                .whereNotNull(TableColumns.Sounds.CREATED_AT)
                .whereEq(TableColumns.Sounds.SHARING, Sharing.from(!isPrivate).value())
                .whereEq(TableColumns.Sounds.TITLE, title)), counts(1));
    }

    private ApiPlaylist insertPostedPlaylist() {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistPost(apiPlaylist.getUrn().getNumericId(), dateProvider.getCurrentDate().getTime(), false);
        return apiPlaylist;
    }

    private PropertySet playlistForTrackPropertySet(ApiPlaylist apiPlaylist, boolean isAdded) {
        return PropertySet.from(
                TrackInPlaylistProperty.URN.bind(apiPlaylist.getUrn()),
                TrackInPlaylistProperty.TITLE.bind(apiPlaylist.getTitle()),
                TrackInPlaylistProperty.TRACK_COUNT.bind(apiPlaylist.getTrackCount()),
                TrackInPlaylistProperty.ADDED_TO_URN.bind(isAdded)
        );
    }

}
