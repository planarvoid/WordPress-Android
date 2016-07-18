package com.soundcloud.android.stations;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.SwipeRefreshAttacher;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.FragmentRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;

import java.util.Arrays;
import java.util.List;

public class StationInfoPresenterTest extends AndroidUnitTest {

    @Rule public final FragmentRule fragmentRule = new FragmentRule(R.layout.default_recyclerview_with_refresh,
                                                                    fragmentArgs());

    private final static Urn TRACK_STATION = Urn.forTrackStation(123L);

    private StationInfoPresenter presenter;

    @Mock StationTrackOperations stationTrackOperations;
    @Mock SwipeRefreshAttacher swipeRefreshAttacher;
    @Mock StationsOperations stationOperations;
    @Mock StationInfoAdapter adapter;

    @Before
    public void setUp() throws Exception {
        presenter = new StationInfoPresenter(stationTrackOperations, swipeRefreshAttacher, adapter, stationOperations);
    }

    @Test
    public void shouldLoadInitialItemsInOnCreate() {
        final StationRecord stationRecord = StationFixtures.getStation(TRACK_STATION);
        final List<StationInfoTrack> stationTracks = StationFixtures.getStationTracks(10);

        when(stationOperations.station(TRACK_STATION)).thenReturn(Observable.just(stationRecord));
        when(stationTrackOperations.initialStationTracks(TRACK_STATION)).thenReturn(Observable.just(stationTracks));

        presenter.onCreate(fragmentRule.getFragment(), null);

        verify(adapter).onNext(Arrays.asList(StationInfo.from(stationRecord), StationInfoTracksBucket.from(stationTracks)));
    }

    private static Bundle fragmentArgs() {
        Bundle bundle = new Bundle();
        bundle.putParcelable(StationInfoFragment.EXTRA_URN, TRACK_STATION);
        return bundle;
    }
}
