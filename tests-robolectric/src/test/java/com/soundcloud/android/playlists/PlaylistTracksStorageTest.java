package com.soundcloud.android.playlists;

import static org.mockito.Mockito.when;

import com.soundcloud.android.api.model.ApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

    private static final Date ADDED_AT = new Date();
    private static final Urn TRACK_URN = Urn.forTrack(123L);

    private PlaylistTracksStorage playlistTracksStorage;

    @Before
    public void setUp() throws Exception {
        playlistTracksStorage = new PlaylistTracksStorage(propeller(), dateProvider);
        when(dateProvider.getCurrentDate()).thenReturn(ADDED_AT);
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
}
