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
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.java.collections.PropertySet;
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
        final TestObserver<List<AddTrackToPlaylistItem>> testObserver = new TestObserver<>();
        final ApiPlaylist apiPlaylist1 = insertPostedPlaylist();
        final ApiPlaylist apiPlaylist2 = insertPostedPlaylist();
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(apiPlaylist2.getUrn(), 0);

        playlistTracksStorage.loadAddTrackToPlaylistItems(apiTrack.getUrn())
                .subscribe(testObserver);

        List<AddTrackToPlaylistItem> result = testObserver.getOnNextEvents().get(0);
        expect(result).toContainExactly(
                createAddTrackToPlaylistItem(apiPlaylist1, false),
                createAddTrackToPlaylistItem(apiPlaylist2, true));
    }

    @Test
    public void addTrackToPlaylistItemsLoadsPlaylistWithCorrectIsAddedStatusForLocallyRemovedTracks() {
        final TestObserver<List<AddTrackToPlaylistItem>> testObserver = new TestObserver<>();
        final ApiPlaylist apiPlaylist = insertPostedPlaylist();
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrackPendingRemoval(apiPlaylist, 0, new Date());

        playlistTracksStorage.loadAddTrackToPlaylistItems(apiTrack.getUrn())
                .subscribe(testObserver);

        List<AddTrackToPlaylistItem> result = testObserver.getOnNextEvents().get(0);
        expect(result).toContainExactly(createAddTrackToPlaylistItem(apiPlaylist, false));
    }

    @Test
    public void addTrackToPlaylistItemsDoNotIncludeRepostedPlaylists() {
        final TestObserver<List<AddTrackToPlaylistItem>> testObserver = new TestObserver<>();
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertPlaylistRepost(apiPlaylist.getId(), dateProvider.getCurrentDate().getTime());

        playlistTracksStorage.loadAddTrackToPlaylistItems(apiTrack.getUrn()).subscribe(testObserver);

        List<AddTrackToPlaylistItem> result = testObserver.getOnNextEvents().get(0);
        expect(result).toBeEmpty();
    }

    @Test
    public void addTrackToPlaylistItemsLoadCorrectPlaylistOfflineState() {
        final TestObserver<List<AddTrackToPlaylistItem>> testObserver = new TestObserver<>();

        final ApiPlaylist offlinePlaylist = insertPostedPlaylist();
        testFixtures().insertPlaylistMarkedForOfflineSync(offlinePlaylist);

        final ApiPlaylist normalPlaylist = insertPostedPlaylist();

        playlistTracksStorage.loadAddTrackToPlaylistItems(Urn.forTrack(123)).subscribe(testObserver);

        List<AddTrackToPlaylistItem> result = testObserver.getOnNextEvents().get(0);
        expect(result).toContainExactly(
                createAddTrackToPlaylistItem(offlinePlaylist, false, true),
                createAddTrackToPlaylistItem(normalPlaylist, false));
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
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        testFixtures().insertPolicyMidTierMonetizable(apiTrack3.getUrn());

        final List<PropertySet> tracks = playlistTracksStorage.playlistTracks(apiPlaylist.getUrn()).toBlocking().single();

        expect(tracks).toContainExactly(
                fromApiTrack(apiTrack1),
                fromApiTrack(apiTrack2),
                expectedMidTierMonetizableTrackFor(apiTrack3)
        );
    }

    @Test
    public void doesNotIncludeTracksFromOtherPlaylists() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrackOther = testFixtures().insertPlaylistTrack(testFixtures().insertPlaylist(), 0);

        final List<PropertySet> tracks = playlistTracksStorage.playlistTracks(apiPlaylist.getUrn()).toBlocking().single();

        expect(tracks).not.toContain(fromApiTrack(apiTrackOther));
    }

    @Test
    public void returnsPlaylistTracksWithOfflineInfo() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        testFixtures().insertCompletedTrackDownload(apiTrack1.getUrn(), 0, 100L);
        testFixtures().insertTrackPendingDownload(apiTrack2.getUrn(), 200L);
        testFixtures().insertTrackDownloadPendingRemoval(apiTrack3.getUrn(), 300L);

        final List<PropertySet> tracks = playlistTracksStorage.playlistTracks(apiPlaylist.getUrn()).toBlocking().single();

        expect(tracks).toContain(
                fromApiTrack(apiTrack1)
                        .put(OfflineProperty.OFFLINE_STATE, OfflineState.DOWNLOADED),
                fromApiTrack(apiTrack2)
                        .put(OfflineProperty.OFFLINE_STATE, OfflineState.REQUESTED),
                fromApiTrack(apiTrack3)
                        .put(OfflineProperty.OFFLINE_STATE, OfflineState.NO_OFFLINE)
        );
    }

    private PropertySet fromApiTrack(ApiTrack apiTrack) {
        return PropertySet.from(
                TrackProperty.URN.bind(apiTrack.getUrn()),
                TrackProperty.TITLE.bind(apiTrack.getTitle()),
                TrackProperty.DURATION.bind(apiTrack.getDuration()),
                TrackProperty.PLAY_COUNT.bind(apiTrack.getStats().getPlaybackCount()),
                TrackProperty.LIKES_COUNT.bind(apiTrack.getStats().getLikesCount()),
                TrackProperty.IS_PRIVATE.bind(apiTrack.isPrivate()),
                TrackProperty.CREATOR_NAME.bind(apiTrack.getUserName()),
                TrackProperty.CREATOR_URN.bind(apiTrack.getUser().getUrn()),
                TrackProperty.SUB_MID_TIER.bind(false)
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

    private AddTrackToPlaylistItem createAddTrackToPlaylistItem(ApiPlaylist playlist, boolean isTrackAdded) {
        return createAddTrackToPlaylistItem(playlist, isTrackAdded, false);
    }

    private AddTrackToPlaylistItem createAddTrackToPlaylistItem(ApiPlaylist playlist, boolean isTrackAdded, boolean isOffline) {
        return new AddTrackToPlaylistItem(
                playlist.getUrn(),
                playlist.getTitle(),
                playlist.getTrackCount(),
                !playlist.isPublic(),
                isOffline,
                isTrackAdded);
    }

    private PropertySet expectedMidTierMonetizableTrackFor(ApiTrack track) {
        return fromApiTrack(track).put(TrackProperty.SUB_MID_TIER, true);
    }

}
