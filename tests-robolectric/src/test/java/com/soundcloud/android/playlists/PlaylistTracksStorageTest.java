package com.soundcloud.android.playlists;

import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.Table;
import com.soundcloud.android.storage.TableColumns;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.DateProvider;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import java.util.Arrays;
import java.util.Date;

@RunWith(SoundCloudTestRunner.class)
public class PlaylistTracksStorageTest extends StorageIntegrationTest {

    @Mock private DateProvider dateProvider;
    @Mock private AccountOperations accountOperations;

    private static final Date ADDED_AT = new Date();
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private PlaylistTracksStorage playlistTracksStorage;

    @Before
    public void setUp() throws Exception {
        playlistTracksStorage = new PlaylistTracksStorage(testScheduler(), dateProvider, accountOperations);
        when(dateProvider.getCurrentDate()).thenReturn(ADDED_AT);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(321L));
    }

    @Test
    public void addsTrackToAPlaylistReturnsChangeSetWithUpdatedTrackCount() {
        final TestObserver<PropertySet> testObserver = new TestObserver<>();
        final ApiPlaylist apiPlaylist = testFixtures().insertEmptyPlaylist();

        playlistTracksStorage.addTrackToPlaylist(apiPlaylist.getUrn(), TRACK_URN)
                .subscribeOn(Schedulers.immediate())
                .subscribe(testObserver);

        testObserver.assertReceivedOnNext(Arrays.asList(
                PropertySet.from(
                        PlaylistProperty.URN.bind(apiPlaylist.getUrn()),
                        PlaylistProperty.TRACK_COUNT.bind(apiPlaylist.getTrackCount() + 1))
        ));
    }

    @Test
    public void addsTrackToPlaylistWritesTrackToPlaylistTracksTable() {
        final TestObserver<PropertySet> testObserver = new TestObserver<>();
        final ApiPlaylist apiPlaylist = testFixtures().insertEmptyPlaylist();

        playlistTracksStorage.addTrackToPlaylist(apiPlaylist.getUrn(), TRACK_URN)
                .subscribeOn(Schedulers.immediate())
                .subscribe(testObserver);

        databaseAssertions().assertPlaylistTracklist(apiPlaylist.getUrn().getNumericId(), Arrays.asList(TRACK_URN));
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

    private void assertPlaylistInserted(long playlistId, String title, boolean isPrivate) {
        assertThat(select(from(Table.Sounds.name())
                .whereEq(TableColumns.Sounds._ID, playlistId)
                .whereEq(TableColumns.Sounds._TYPE, TableColumns.Sounds.TYPE_PLAYLIST)
                .whereEq(TableColumns.Sounds.USER_ID, 321L)
                .whereNotNull(TableColumns.Sounds.CREATED_AT)
                .whereEq(TableColumns.Sounds.SHARING, Sharing.from(!isPrivate).value())
                .whereEq(TableColumns.Sounds.TITLE, title)), counts(1));
    }
}
