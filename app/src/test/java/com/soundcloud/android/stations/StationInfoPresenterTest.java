package com.soundcloud.android.stations;

import static com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem.createTrack;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.stations.StationInfoAdapter.StationInfoClickListener;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

public class StationInfoPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule1 =
            new FragmentRule(R.layout.default_recyclerview_with_refresh, fragmentArgs());
    @Rule public final FragmentRule fragmentRule2 =
            new FragmentRule(R.layout.default_recyclerview_with_refresh, fragmentArgsNoDiscoverySource());


    private final static Urn TRACK_STATION = Urn.forTrackStation(123L);

    private StationInfoPresenter presenter;

    @Mock SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock StationsOperations stationOperations;
    @Mock StationInfoAdapterFactory adapterFactory;
    @Mock StationInfoAdapter adapter;
    @Mock StationInfoTracksBucketRendererFactory rendererFactory;
    @Mock StationInfoTracksBucketRenderer bucketRenderer;
    @Mock StartStationPresenter stationPresenter;
    @Mock PlayQueueManager playQueueManager;

    private TestEventBus eventBus;
    private StationRecord stationRecord;
    private List<StationInfoTrack> stationTracks;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        stationRecord = StationFixtures.getStation(TRACK_STATION);
        stationTracks = StationFixtures.getStationTracks(10);

        when(playQueueManager.getCollectionUrn()).thenReturn(Urn.NOT_SET);
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(PlayQueueItem.EMPTY);
        when(stationOperations.lastPlayedPosition(TRACK_STATION)).thenReturn(Observable.just(Consts.NOT_SET));
        when(stationOperations.stationWithSeed(TRACK_STATION, Optional.<Urn>absent())).thenReturn(Observable.just(
                stationRecord));
        when(stationOperations.stationTracks(TRACK_STATION)).thenReturn(Observable.just(stationTracks));

        when(rendererFactory.create(any(StationInfoPresenter.class))).thenReturn(bucketRenderer);
        when(adapterFactory.create(any(StationInfoClickListener.class), eq(bucketRenderer))).thenReturn(adapter);

        presenter = new StationInfoPresenter(swipeRefreshAttacher,
                                             adapterFactory,
                                             stationOperations,
                                             stationPresenter,
                                             rendererFactory,
                                             playQueueManager,
                                             eventBus);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        presenter.onCreate(fragmentRule1.getFragment(), null);

        verify(adapter).onNext(Arrays.asList(StationInfo.from(stationRecord),
                                             StationInfoTracksBucket.from(stationTracks, Consts.NOT_SET)));
    }

    @Test
    public void shouldSubscribeAdapterToPlayingTrackChanges() {
        final Urn playingTrackUrn = stationTracks.get(0).getUrn();
        presenter.onCreate(fragmentRule1.getFragment(), null);

        eventBus.publish(EventQueue.CURRENT_PLAY_QUEUE_ITEM, positionChangedEvent(playingTrackUrn));

        verify(adapter).updateNowPlaying(playingTrackUrn);
    }

    @Test
    public void shouldStartStationOnPlayButtonClicked() {
        presenter.onCreate(fragmentRule1.getFragment(), null);

        presenter.onPlayButtonClicked(context());

        verify(stationPresenter).startStation(context(), TRACK_STATION, DiscoverySource.STATIONS_SUGGESTIONS);
    }

    @Test
    public void shouldStartStationOnPlayButtonWithDefaultDiscoverySource() {
        presenter.onCreate(fragmentRule2.getFragment(), null);

        presenter.onPlayButtonClicked(context());

        verify(stationPresenter).startStation(context(), TRACK_STATION, DiscoverySource.STATIONS);
    }

    private CurrentPlayQueueItemEvent positionChangedEvent(Urn trackUrn) {
        return CurrentPlayQueueItemEvent.fromPositionChanged(createTrack(trackUrn), TRACK_STATION, 0);
    }

    private static Bundle fragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(StationInfoFragment.EXTRA_URN, TRACK_STATION);
        bundle.putString(StationInfoFragment.EXTRA_SOURCE, DiscoverySource.STATIONS_SUGGESTIONS.value());
        return bundle;
    }

    private static Bundle fragmentArgsNoDiscoverySource() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(StationInfoFragment.EXTRA_URN, TRACK_STATION);
        return bundle;
    }
}