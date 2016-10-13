package com.soundcloud.android.stations;

import static com.soundcloud.android.stations.StationFixtures.getStationWithTracks;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.propeller.ChangeResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.view.View;

public class StationMenuPresenterTest extends AndroidUnitTest {

    private static final StationWithTracks STATION = getStationWithTracks(Urn.forTrackStation(123L));

    @Mock private StationsOperations stationsOperations;
    @Mock private StationMenuRendererFactory stationMenuRenderFactory;
    @Mock private StationMenuRenderer stationMenuRenderer;
    @Mock private View button;

    private StationMenuPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(stationsOperations.stationWithTracks(any(Urn.class), eq(Optional.<Urn>absent())))
                .thenReturn(Observable.just(STATION));
        when(stationsOperations.toggleStationLike(any(Urn.class), anyBoolean()))
                .thenReturn(Observable.<ChangeResult>empty());

        presenter = new StationMenuPresenter(context(), stationMenuRenderFactory, stationsOperations);

        when(stationMenuRenderFactory.create(presenter, button)).thenReturn(stationMenuRenderer);
    }

    @Test
    public void clickingOnAddToLikesAddStationLike() {
        final PublishSubject<ChangeResult> likeObservable = PublishSubject.create();

        when(stationsOperations.toggleStationLike(STATION.getUrn(), !STATION.isLiked()))
                .thenReturn(likeObservable);

        presenter.show(button, STATION.getUrn());
        presenter.handleLike(STATION);

        assertThat(likeObservable.hasObservers()).isTrue();
    }
}
