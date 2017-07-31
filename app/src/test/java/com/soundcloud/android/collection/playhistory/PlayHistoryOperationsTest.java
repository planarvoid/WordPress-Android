package com.soundcloud.android.collection.playhistory;

import static com.soundcloud.android.testsupport.fixtures.ModelFixtures.trackItem;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.sync.NewSyncOperations;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackRepository;
import com.soundcloud.java.collections.Lists;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PlayHistoryOperationsTest {

    private static final List<Track> TRACKS = ModelFixtures.tracks(10);
    private static final List<Urn> TRACK_URNS = Lists.transform(TRACKS, Track::urn);
    private static final List<Urn> AVAILABLE_TRACK_URNS = TRACK_URNS.subList(0, 5);
    private static final Screen screen = Screen.DISCOVER;
    private static final Urn TRACK_URN = TRACKS.get(0).urn();

    @Mock private PlaybackInitiator playbackInitiator;
    @Mock private PlayHistoryStorage playHistoryStorage;
    @Mock private NewSyncOperations syncOperations;
    @Mock private ClearPlayHistoryCommand clearCommand;
    @Mock private TrackRepository trackRepository;
    @Captor private ArgumentCaptor<Single<List<Urn>>> playTracksCaptor;

    private Scheduler scheduler = Schedulers.trampoline();
    private List<TrackItem> trackItems;

    private PlayHistoryOperations operations;

    @Before
    public void setUp() throws Exception {
        final List<Urn> urns = Lists.transform(TRACKS, Track::urn);
        when(playHistoryStorage.loadTrackUrns(anyInt())).thenReturn(Single.just(urns));
        when(trackRepository.trackListFromUrns(urns)).thenReturn(Single.just(TRACKS));
        trackItems = new ArrayList<>(TRACKS.size());
        for (Track track : TRACKS) {
            trackItems.add(trackItem(track));
        }

        operations = new PlayHistoryOperations(playbackInitiator,
                                               playHistoryStorage,
                                               scheduler,
                                               syncOperations,
                                               clearCommand,
                                               ModelFixtures.entityItemCreator(),
                                               trackRepository);
    }

    @Test
    public void shouldReturnPlayHistory() throws Exception {
        when(syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY))
                .thenReturn(Single.just(SyncResult.noOp()));

        operations.playHistory().test()
                  .assertValue(trackItems)
                  .assertComplete();
    }

    @Test
    public void shouldForceSyncPlayHistoryOnRefresh() {
        when(syncOperations.lazySyncIfStale(Syncable.PLAY_HISTORY))
                .thenReturn(Single.just(SyncResult.noOp()));

        operations.playHistory().test()
                  .assertValue(trackItems)
                  .assertComplete();
    }

    @Test
    public void shouldClearHistory() {
        when(clearCommand.toSingle()).thenReturn(Single.just(true));

        operations.clearHistory().test()
                  .assertValue(true);
    }

    @Test
    public void shouldFilterOutUnavailableTracks() {
        when(playHistoryStorage.loadTrackUrnsForPlayback()).thenReturn(Single.just(TRACK_URNS));
        final Single<List<Urn>> availableTracksSingle = Single.just(AVAILABLE_TRACK_URNS);
        when(trackRepository.availableTracks(TRACK_URNS)).thenReturn(availableTracksSingle);
        operations.startPlaybackFrom(TRACK_URN, screen);

        verify(playbackInitiator).playTracks(playTracksCaptor.capture(), eq(TRACK_URN), eq(0), eq(PlaySessionSource.forHistory(screen)));

        playTracksCaptor.getValue().test().assertValue(AVAILABLE_TRACK_URNS);
    }
}
