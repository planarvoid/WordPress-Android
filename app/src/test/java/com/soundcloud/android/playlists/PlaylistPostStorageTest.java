package com.soundcloud.android.playlists;

import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_ID;
import static com.soundcloud.android.storage.TableColumns.Activities.SOUND_TYPE;
import static com.soundcloud.android.storage.Tables.Sounds.TYPE_PLAYLIST;
import static com.soundcloud.android.testsupport.InjectionSupport.providerOf;
import static com.soundcloud.propeller.query.Query.from;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.sync.activities.ApiPlaylistRepostActivity;
import com.soundcloud.android.sync.likes.ApiLike;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.test.assertions.QueryAssertions;
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

    private TestSubscriber<List<PlaylistAssociation>> subscriber = new TestSubscriber<>();

    @Mock private RemovePlaylistCommand removePlaylistCommand;

    @Before
    public void setUp() throws Exception {
        storage = new PlaylistPostStorage(propellerRx(),
                                          new TestDateProvider(),
                                          removePlaylistCommand,
                                          new PlaylistAssociationMapperFactory(providerOf(new NewPlaylistMapper())));
    }

    @Test
    public void shouldLoadAllPlaylistPosts() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void markPendingRemovalShouldFilterOutPlaylistWhenLoading() {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);

        storage.markPendingRemoval(playlist1.getPlaylist().urn()).subscribe();
        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        databaseAssertions().assertPlaylistInserted(playlist1.getPlaylist().urn());
        subscriber.assertValue(Collections.singletonList(playlist2));
    }

    @Test
    public void shouldReturnTrackCountAsMaximumOfRemoteAndLocalCounts() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        createPlaylistPostAt(POSTED_DATE_2);
        assertThat(playlist1.getPlaylist().trackCount()).isEqualTo(2);

        final Urn playlistUrn = playlist1.getPlaylist().urn();
        testFixtures().insertPlaylistTrack(playlistUrn, 0);
        testFixtures().insertPlaylistTrack(playlistUrn, 1);
        testFixtures().insertPlaylistTrack(playlistUrn, 2);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        final List<PlaylistAssociation> result = subscriber.getOnNextEvents().get(0);
        assertThat(result.get(1).getPlaylist().urn()).isEqualTo(playlistUrn);
        assertThat(result.get(1).getPlaylist().trackCount()).isEqualTo(3);
    }

    @Test
    public void shouldAdhereToLimit() throws Exception {
        createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        storage.loadPostedPlaylists(1, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist2));
    }

    @Test
    public void shouldAdhereToPostedTime() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistRepostAt(POSTED_DATE_1, POSTED_DATE_1);
        createPlaylistRepostAt(POSTED_DATE_3, POSTED_DATE_1);

        storage.loadPostedPlaylists(2, POSTED_DATE_2.getTime(), Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }

    @Test
    public void shouldAdhereToPlaylistTitleFilter() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(2, Long.MAX_VALUE, playlist1.getPlaylist().title()).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }

    @Test
    public void shouldAdhereToPlaylistUserFilter() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(2, Long.MAX_VALUE, playlist1.getPlaylist().creatorName()).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }

    @Test
    public void shouldAdhereToPlaylistTrackTitleFilter() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1.getPlaylist().urn(), 0);
        createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(2, Long.MAX_VALUE, apiTrack.getTitle()).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }


    @Test
    public void shouldAdhereToPlaylistTrackTitleFilterWithSingleResult() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1.getPlaylist().urn(), 0);
        final ApiTrack apiTrack2 = testFixtures().insertTrackWithTitle(apiTrack.getTitle());
        testFixtures().insertPlaylistTrack(playlist1.getPlaylist().urn(), apiTrack2.getUrn(), 1);

        storage.loadPostedPlaylists(2, Long.MAX_VALUE, apiTrack.getTitle()).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }

    @Test
    public void shouldAdhereToPlaylistTrackUserFilter() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final ApiTrack apiTrack = testFixtures().insertPlaylistTrack(playlist1.getPlaylist().urn(), 0);
        createPlaylistPostAt(POSTED_DATE_2);

        storage.loadPostedPlaylists(2, Long.MAX_VALUE, apiTrack.getUserName()).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(playlist1));
    }

    @Test
    public void shouldIncludeLikedStatus() throws Exception {
        final PlaylistAssociation playlistAssociation = createPlaylistPostAt(POSTED_DATE_1);
        testFixtures().insertLike(ApiLike.create(playlistAssociation.getPlaylist().urn(), new Date()));
        final PlaylistAssociation expected = PlaylistAssociation.create(playlistBuilder(playlistAssociation)
                                                                                .isMarkedForOffline(false)
                                                                                .isLikedByCurrentUser(true)
                                                                                .isRepostedByCurrentUser(false)
                                                                                .build(),
                                                                        playlistAssociation.getCreatedAt());


        storage.loadPostedPlaylists(1, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(expected));
    }

    @Test
    public void shouldIncludeRepostedStatus() throws Exception {
        final PlaylistAssociation playlistAssociation = createPlaylistPostAt(POSTED_DATE_1);
        testFixtures().insertPlaylistRepost(playlistAssociation.getPlaylist().urn().getNumericId(), new Date().getTime());
        final PlaylistAssociation expected = PlaylistAssociation.create(playlistBuilder(playlistAssociation)
                                                                                .isMarkedForOffline(false)
                                                                                .isLikedByCurrentUser(false)
                                                                                .isRepostedByCurrentUser(true)
                                                                                .build(),
                                                                        playlistAssociation.getCreatedAt());


        storage.loadPostedPlaylists(1, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Collections.singletonList(expected));
    }

    @Test
    public void shouldNotIncludePlaylistsNotPresentInTheCollectionItemsTable() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        createPlaylistAt(POSTED_DATE_3); // deleted

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void shouldNotConfuseTracksForPlaylists() throws Exception {
        final PlaylistAssociation playlist1 = createPlaylistPostAt(POSTED_DATE_1);
        final PlaylistAssociation playlist2 = createPlaylistPostAt(POSTED_DATE_2);
        createTrackWithId(9999);
        createPlaylistCollectionWithId(9999, new Date());

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(playlist2, playlist1));
    }

    @Test
    public void loadRequestedDownloadStateWhenPlaylistIsMarkedForOfflineAndHasDownloadRequests() {
        final ApiPlaylist postedPlaylist = insertPlaylistWithRequestedDownload(POSTED_DATE_1,
                                                                               System.currentTimeMillis());

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(
                Collections.singletonList(createPostedPlaylist(postedPlaylist, OfflineState.REQUESTED)));
    }

    @Test
    public void loadDownloadedStateWhenPlaylistIsMarkedForOfflineAndNoDownloadRequest() {
        final ApiPlaylist postedPlaylist = insertPlaylistWithDownloadedTrack(POSTED_DATE_1,
                                                                             123L,
                                                                             System.currentTimeMillis());

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(
                Collections.singletonList(createPostedPlaylist(postedPlaylist, OfflineState.DOWNLOADED)));
    }

    @Test
    public void loadDownloadStateOfAPlaylistWithOnlyCreatorOptOutTracks() {
        final ApiPlaylist postedPlaylist = insertPlaylistWithUnavailableTrack(POSTED_DATE_1, 123L);
        addCreatorOptOutTrackToPlaylist(postedPlaylist);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(
                Collections.singletonList(createPostedPlaylist(postedPlaylist, OfflineState.UNAVAILABLE)));
    }

    @Test
    public void loadDownloadStateOfAPlaylistWithSomeCreatorOptOutTracks() {
        final ApiPlaylist postedPlaylist = insertPlaylistWithDownloadedTrack(POSTED_DATE_1,
                                                                             123L,
                                                                             System.currentTimeMillis());
        addCreatorOptOutTrackToPlaylist(postedPlaylist);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(
                Collections.singletonList(createPostedPlaylist(postedPlaylist, OfflineState.DOWNLOADED)));
    }

    @Test
    public void loadDownloadedStateOfTwoDifferentPlaylistsDoesNotInfluenceEachOther() {
        final ApiPlaylist postedPlaylistDownloaded = insertPlaylistWithDownloadedTrack(POSTED_DATE_1,
                                                                                       123L,
                                                                                       System.currentTimeMillis());
        final PlaylistAssociation downloadedPlaylist = createPostedPlaylist(postedPlaylistDownloaded,
                                                                            OfflineState.DOWNLOADED);

        final ApiPlaylist postedPlaylistRequested = insertPlaylistWithRequestedDownload(POSTED_DATE_2,
                                                                                        System.currentTimeMillis());
        final PlaylistAssociation requestedPlaylist = createPostedPlaylist(postedPlaylistRequested,
                                                                           OfflineState.REQUESTED);

        storage.loadPostedPlaylists(10, Long.MAX_VALUE, Strings.EMPTY).subscribe(subscriber);

        subscriber.assertValue(Arrays.asList(requestedPlaylist, downloadedPlaylist));
    }

    @Test
    public void removeAssociatedActivitiesWhenMarkingPlaylistPendingRemovals() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        final ApiPlaylistRepostActivity apiActivityItem = ModelFixtures.apiPlaylistRepostActivity(playlist);
        testFixtures().insertPlaylistRepostActivity(apiActivityItem);

        storage.markPendingRemoval(playlist.getUrn()).subscribe();

        final Query query = from(Table.Activities)
                .whereEq(SOUND_ID, playlist.getId())
                .whereEq(SOUND_TYPE, TYPE_PLAYLIST);

        QueryAssertions.assertThat(select(query)).isEmpty();
    }

    @Test
    public void removedSoundStreamEntryAssociatedWithRemovedPlaylist() {
        final ApiPlaylist playlist = ModelFixtures.create(ApiPlaylist.class);
        testFixtures().insertStreamPlaylistPost(playlist.getId(), 123L);

        storage.markPendingRemoval(playlist.getUrn()).subscribe();

        final Query query = from(Table.SoundStream)
                .whereEq(TableColumns.SoundStream.SOUND_ID, playlist.getId())
                .whereEq(TableColumns.SoundStream.SOUND_TYPE, TYPE_PLAYLIST);

        QueryAssertions.assertThat(select(query)).isEmpty();
    }

    private Playlist.Builder playlistBuilder(PlaylistAssociation playlistAssociation) {
        return ModelFixtures.playlistBuilder(playlistAssociation.getPlaylist());
    }

    private PlaylistAssociation createPostedPlaylist(ApiPlaylist apiPlaylist, OfflineState offlineState) {
        final PlaylistAssociation postedPlaylist = createPostedPlaylist(apiPlaylist);
        return PlaylistAssociation.create(playlistBuilder(postedPlaylist)
                                                  .isMarkedForOffline(true)
                                                  .offlineState(offlineState)
                                                  .isLikedByCurrentUser(false)
                                                  .isRepostedByCurrentUser(false)
                                                  .build(),
                                          postedPlaylist.getCreatedAt());
    }

    private ApiPlaylist insertPlaylistWithRequestedDownload(Date postedDate, long requestedAt) {
        final ApiPlaylist postedPlaylist = insertPlaylistMarkedForOffline(postedDate);

        final ApiTrack track = testFixtures().insertPlaylistTrack(postedPlaylist, 0);
        testFixtures().insertTrackPendingDownload(track.getUrn(), requestedAt);
        return postedPlaylist;
    }

    private ApiPlaylist insertPlaylistWithDownloadedTrack(Date postedDate, long requestedAt, long completedAt) {
        final ApiPlaylist postedPlaylistDownloaded = insertPlaylistMarkedForOffline(postedDate);

        final ApiTrack trackDownloaded = testFixtures().insertPlaylistTrack(postedPlaylistDownloaded, 0);
        testFixtures().insertCompletedTrackDownload(trackDownloaded.getUrn(), requestedAt, completedAt);
        return postedPlaylistDownloaded;
    }

    private ApiPlaylist insertPlaylistWithUnavailableTrack(Date postedDate, long unavailableAt) {
        final ApiPlaylist postedPlaylistDownloaded = insertPlaylistMarkedForOffline(postedDate);

        final ApiTrack trackDownloaded = testFixtures().insertPlaylistTrack(postedPlaylistDownloaded, 0);
        testFixtures().insertUnavailableTrackDownload(trackDownloaded.getUrn(), unavailableAt);
        return postedPlaylistDownloaded;
    }

    private ApiPlaylist insertPlaylistMarkedForOffline(Date postedDate) {
        final ApiPlaylist postedPlaylistDownloaded = createPlaylistAt(postedDate);
        createPlaylistCollectionWithId(postedPlaylistDownloaded.getUrn().getNumericId(), postedDate);
        testFixtures().insertPlaylistMarkedForOfflineSync(postedPlaylistDownloaded);
        return postedPlaylistDownloaded;
    }

    private PlaylistAssociation createPlaylistPostAt(Date postedAt) {
        return createPlaylistRepostAt(postedAt, postedAt);
    }

    private PlaylistAssociation createPlaylistRepostAt(Date postedAt, Date createdAt) {
        final ApiPlaylist apiPlaylist = createPlaylistAt(createdAt);
        final Playlist.Builder playlist = ModelFixtures.playlistBuilder(apiPlaylist)
                                                       .isLikedByCurrentUser(false)
                                                       .isRepostedByCurrentUser(false)
                                                       .isMarkedForOffline(false);

        createPlaylistCollectionWithId(apiPlaylist.getUrn().getNumericId(), postedAt);
        return createPostedPlaylist(playlist, createdAt);
    }

    private PlaylistAssociation createPostedPlaylist(ApiPlaylist apiPlaylist) {
        final Playlist.Builder builder = ModelFixtures.playlistBuilder(apiPlaylist);
        return createPostedPlaylist(builder, apiPlaylist.getCreatedAt());
    }

    private PlaylistAssociation createPostedPlaylist(Playlist.Builder builder, Date createdAt) {
        final Playlist playlist = builder
                .isLikedByCurrentUser(false)
                .build();
        return PlaylistAssociation.create(playlist, createdAt);
    }

    private ApiPlaylist createPlaylistAt(Date creationDate) {
        return testFixtures().insertPlaylistWithCreatedAt(creationDate);
    }

    private void createPlaylistCollectionWithId(long playlistId, Date postedAt) {
        testFixtures().insertPlaylistPost(playlistId, postedAt.getTime(), false);
    }

    private void createTrackWithId(long trackId) {
        ApiTrack apiTrack = ModelFixtures.create(ApiTrack.class);
        apiTrack.setUrn(Urn.forTrack(trackId));
        testFixtures().insertTrack(apiTrack);
        testFixtures().insertTrackPost(apiTrack.getId(), apiTrack.getCreatedAt().getTime(), false);
    }

    private void addCreatorOptOutTrackToPlaylist(ApiPlaylist postedPlaylist) {
        final ApiTrack track = testFixtures().insertPlaylistTrack(postedPlaylist, 1);
        testFixtures().insertUnavailableTrackDownload(track.getUrn(), POSTED_DATE_1.getTime());
    }
}
