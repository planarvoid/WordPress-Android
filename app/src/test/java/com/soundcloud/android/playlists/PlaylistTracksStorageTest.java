package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.TestDateProvider;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class PlaylistTracksStorageTest extends StorageIntegrationTest {

    @Mock private AccountOperations accountOperations;

    private static final Date ADDED_AT = new Date();
    private PlaylistTracksStorage playlistTracksStorage;
    private TestDateProvider dateProvider;

    @Before
    public void setUp() throws Exception {
        dateProvider = new TestDateProvider(ADDED_AT);
        playlistTracksStorage = new PlaylistTracksStorage(propellerRx(),
                                                          dateProvider,
                                                          accountOperations);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(321L));
    }

    @Test
    public void playlistForAddingTrackLoadsPlaylistWithCorrectOrder() {
        final TestSubscriber<List<AddTrackToPlaylistItem>> testSubscriber = new TestSubscriber<>();
        final ApiPlaylist apiPlaylist1 = testFixtures().insertPostedPlaylist(ADDED_AT);
        final ApiPlaylist apiPlaylist2 = testFixtures().insertPostedPlaylist(new Date(ADDED_AT.getTime() - 1000));

        playlistTracksStorage.loadAddTrackToPlaylistItems(Urn.forTrack(123L))
                             .subscribe(testSubscriber);

        testSubscriber.assertValues(Arrays.asList(
                createAddTrackToPlaylistItem(apiPlaylist1, false),
                createAddTrackToPlaylistItem(apiPlaylist2, false)));
    }

    @Test
    public void playlistForAddingTrackLoadsPlaylistWithCorrectIsAddedStatus() {
        final TestSubscriber<List<AddTrackToPlaylistItem>> testSubscriber = new TestSubscriber<>();
        final ApiPlaylist apiPlaylist1 = insertPostedPlaylist();
        final ApiPlaylist apiPlaylist2 = insertPostedPlaylist();
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(apiPlaylist2.getUrn(), 0);

        playlistTracksStorage.loadAddTrackToPlaylistItems(apiTrack.getUrn())
                             .subscribe(testSubscriber);

        testSubscriber.assertValues(Arrays.asList(
                createAddTrackToPlaylistItem(apiPlaylist1, false),
                createAddTrackToPlaylistItem(apiPlaylist2, true)));
    }

    @Test
    public void addTrackToPlaylistItemsLoadsPlaylistWithCorrectIsAddedStatusForLocallyRemovedTracks() {
        final TestSubscriber<List<AddTrackToPlaylistItem>> testSubscriber = new TestSubscriber<>();
        final ApiPlaylist apiPlaylist = insertPostedPlaylist();
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrackPendingRemoval(apiPlaylist, 0, new Date());

        playlistTracksStorage.loadAddTrackToPlaylistItems(apiTrack.getUrn())
                             .subscribe(testSubscriber);

        testSubscriber.assertValues(
                Collections.singletonList(createAddTrackToPlaylistItem(apiPlaylist, false)));
    }

    @Test
    public void addTrackToPlaylistItemsDoNotIncludeRepostedPlaylists() {
        final TestSubscriber<List<AddTrackToPlaylistItem>> testSubscriber = new TestSubscriber<>();
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack = testFixtures().insertTrack();
        testFixtures().insertPlaylistRepost(apiPlaylist.getId(), dateProvider.getCurrentDate().getTime());

        playlistTracksStorage.loadAddTrackToPlaylistItems(apiTrack.getUrn()).subscribe(testSubscriber);

        testSubscriber.assertValues(Collections.emptyList());
    }

    @Test
    public void addTrackToPlaylistItemsLoadCorrectPlaylistOfflineState() {
        final TestSubscriber<List<AddTrackToPlaylistItem>> testSubscriber = new TestSubscriber<>();

        final ApiPlaylist offlinePlaylist = insertPostedPlaylist();
        testFixtures().insertPlaylistMarkedForOfflineSync(offlinePlaylist);

        final ApiPlaylist normalPlaylist = insertPostedPlaylist();

        playlistTracksStorage.loadAddTrackToPlaylistItems(Urn.forTrack(123)).subscribe(testSubscriber);

        testSubscriber.assertValues(
                Arrays.asList(
                        createAddTrackToPlaylistItem(offlinePlaylist, false, true),
                        createAddTrackToPlaylistItem(normalPlaylist, false)
                )
        );
    }


    @Test
    public void insertsNewPlaylist() {
        final TestSubscriber<Urn> testSubscriber = new TestSubscriber<>();

        playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))
                             .subscribe(testSubscriber);

        final Urn urn = testSubscriber.getOnNextEvents().get(0);
        assertThat(urn.isPlaylist()).isTrue();
        assertThat(urn.isLocal()).isTrue();
        databaseAssertions().assertPlaylistInserted(urn.getNumericId(), "title", true);
    }

    @Test
    public void insertsPlaylistPost() {
        final TestSubscriber<Urn> testSubscriber = new TestSubscriber<>();

        playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))
                             .subscribe(testSubscriber);

        Urn playlistUrn = testSubscriber.getOnNextEvents().get(0);
        databaseAssertions().assertPlaylistPostInsertedFor(playlistUrn);
    }

    @Test
    public void insertsFirstPlaylistTrack() {
        final TestSubscriber<Urn> testSubscriber = new TestSubscriber<>();

        playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))
                             .subscribe(testSubscriber);

        long playlistId = testSubscriber.getOnNextEvents().get(0).getNumericId();
        databaseAssertions().assertPlaylistTracklist(playlistId, Collections.singletonList(Urn.forTrack(123)));
    }

    private ApiPlaylist insertPostedPlaylist() {
        return testFixtures().insertPostedPlaylist(ADDED_AT);
    }

    private AddTrackToPlaylistItem createAddTrackToPlaylistItem(ApiPlaylist playlist, boolean isTrackAdded) {
        return createAddTrackToPlaylistItem(playlist, isTrackAdded, false);
    }

    private AddTrackToPlaylistItem createAddTrackToPlaylistItem(ApiPlaylist playlist,
                                                                boolean isTrackAdded,
                                                                boolean isOffline) {
        return new AddTrackToPlaylistItem(
                playlist.getUrn(),
                playlist.getTitle(),
                playlist.getTrackCount(),
                !playlist.isPublic(),
                isOffline,
                isTrackAdded);
    }

}
