package com.soundcloud.android.playlists;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.collections.PropertySet;
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
        playlistTracksStorage = new PlaylistTracksStorage(propellerRx(), dateProvider, accountOperations);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(321L));
    }

    @Test
    public void playlistForAddingTrackLoadsPlaylistWithCorrectOrder() {
        final TestSubscriber<List<AddTrackToPlaylistItem>> testSubscriber = new TestSubscriber<>();
        final ApiPlaylist apiPlaylist1 = insertPostedPlaylist(ADDED_AT);
        final ApiPlaylist apiPlaylist2 = insertPostedPlaylist(new Date(ADDED_AT.getTime() - 1000));

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

        testSubscriber.assertValues(Collections.<AddTrackToPlaylistItem>emptyList());
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
    public void loadPlaylistTracksWithUnavailableOfflineStateWhenPlaylistMarkedForOffline() {
        final TestSubscriber<List<PropertySet>> testSubscriber = new TestSubscriber<>();

        final ApiPlaylist offlinePlaylist = insertPostedPlaylist();
        testFixtures().insertPlaylistMarkedForOfflineSync(offlinePlaylist);
        ApiTrack track = testFixtures().insertPlaylistTrack(offlinePlaylist.getUrn(), 0);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), new Date().getTime());

        playlistTracksStorage.playlistTracks(offlinePlaylist.getUrn()).subscribe(testSubscriber);

        List<PropertySet> result = testSubscriber.getOnNextEvents().get(0);
        assertThat(result.get(0).contains(OfflineProperty.OFFLINE_STATE)).isTrue();
    }

    @Test
    public void doesNotLoadUnavailableOfflineStateForPlaylistTracksWhenPlaylistMarkedForOffline() {
        final TestSubscriber<List<PropertySet>> testSubscriber = new TestSubscriber<>();

        final ApiPlaylist normalPlaylist = insertPostedPlaylist();
        ApiTrack track = testFixtures().insertPlaylistTrack(normalPlaylist.getUrn(), 0);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), new Date().getTime());

        playlistTracksStorage.playlistTracks(normalPlaylist.getUrn()).subscribe(testSubscriber);

        List<PropertySet> result = testSubscriber.getOnNextEvents().get(0);
        assertThat(result.get(0).contains(OfflineProperty.OFFLINE_STATE)).isFalse();
    }

    @Test
    public void insertsNewPlaylist() {
        final TestSubscriber<Urn> testSubscriber = new TestSubscriber<>();

        playlistTracksStorage.createNewPlaylist("title", true, Urn.forTrack(123))
                .subscribe(testSubscriber);

        long playlistId = testSubscriber.getOnNextEvents().get(0).getNumericId();
        databaseAssertions().assertPlaylistInserted(playlistId, "title", true);
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

    @Test
    public void returnsPlaylistTracks() throws Exception {
        final ApiPlaylist apiPlaylist = testFixtures().insertPlaylist();
        final ApiTrack apiTrack1 = testFixtures().insertPlaylistTrack(apiPlaylist, 0);
        final ApiTrack apiTrack2 = testFixtures().insertPlaylistTrack(apiPlaylist, 1);
        final ApiTrack apiTrack3 = testFixtures().insertPlaylistTrack(apiPlaylist, 2);

        testFixtures().insertPolicyMidTierMonetizable(apiTrack3.getUrn());

        final List<PropertySet> tracks = playlistTracksStorage.playlistTracks(apiPlaylist.getUrn()).toBlocking().single();

        assertThat(tracks).containsExactly(
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

        assertThat(tracks).doesNotContain(fromApiTrack(apiTrackOther));
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

        assertThat(tracks).contains(
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

    private ApiPlaylist insertPostedPlaylist() {
        return insertPostedPlaylist(ADDED_AT);
    }

    private ApiPlaylist insertPostedPlaylist(Date postedAt) {
        ApiPlaylist apiPlaylist = testFixtures().insertPlaylistWithCreatedAt(postedAt);
        testFixtures().insertPlaylistPost(apiPlaylist.getUrn().getNumericId(), postedAt.getTime(), false);
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
