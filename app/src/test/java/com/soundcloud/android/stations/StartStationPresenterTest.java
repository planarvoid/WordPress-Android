package com.soundcloud.android.stations;

import static com.soundcloud.android.playback.DiscoverySource.STATIONS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.performance.PerformanceMetricsEngine;
import com.soundcloud.android.configuration.experiments.MiniplayerExperiment;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.playback.PlaybackResult;
import com.soundcloud.android.playback.ui.view.PlaybackFeedbackHelper;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.tracks.DelayedLoadingDialogPresenter;
import com.soundcloud.rx.eventbus.EventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Context;
import android.content.DialogInterface;

public class StartStationPresenterTest extends AndroidUnitTest {

    @Mock StationsOperations stationsOperations;
    @Mock PlaybackInitiator playbackInitiator;
    @Mock EventBus eventBus;
    @Mock PlaybackFeedbackHelper playbackFeedbackHelper;
    @Mock ScreenProvider screenProvider;
    @Mock DelayedLoadingDialogPresenter.Builder dialogBuilder;
    @Mock DelayedLoadingDialogPresenter dialogPresenter;
    @Mock MiniplayerExperiment miniplayerExperiment;
    @Mock PerformanceMetricsEngine performanceMetricsEngine;

    private final Screen screen = Screen.SEARCH_MAIN;
    private StartStationPresenter presenter;

    @Before
    public void setUp() {
        when(dialogBuilder.setLoadingMessage(anyString())).thenReturn(dialogBuilder);
        when(dialogBuilder.setOnErrorToastText(anyString())).thenReturn(dialogBuilder);
        when(dialogBuilder.setOnCancelListener(any(DialogInterface.OnCancelListener.class))).thenReturn(dialogBuilder);
        when(dialogBuilder.create()).thenReturn(dialogPresenter);
        when(dialogPresenter.show(any(Context.class))).thenReturn(dialogPresenter);

        when(screenProvider.getLastScreenTag()).thenReturn(screen.get());
        when(playbackInitiator.playStation(any(Urn.class), anyListOf(StationTrack.class),
                                           any(PlaySessionSource.class), any(Urn.class), anyInt()))
                .thenReturn(Observable.just(PlaybackResult.success()));

        presenter = new StartStationPresenter(dialogBuilder,
                                              stationsOperations,
                                              playbackInitiator,
                                              eventBus,
                                              playbackFeedbackHelper,
                                              screenProvider,
                                              miniplayerExperiment,
                                              performanceMetricsEngine);
    }

    @Test
    public void playStationShouldStartFromTheNextTrack() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn, 5, 2);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(screen.get(), stationUrn, STATIONS);
        final Observable<StationRecord> stationObservable = Observable.just(station);

        presenter.playStation(context(), stationObservable, STATIONS);

        verify(playbackInitiator).playStation(stationUrn, station.getTracks(),
                                              playSessionSource, station.getTracks().get(2).getTrackUrn(), 3);
    }

    @Test
    public void playStationShouldRestartFromZeroWhenReachingTheEnd() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn, 5, 4);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(screen.get(), stationUrn, STATIONS);
        final Observable<StationRecord> stationObservable = Observable.just(station);

        presenter.playStation(context(), stationObservable, STATIONS);

        verify(playbackInitiator).playStation(stationUrn, station.getTracks(),
                                              playSessionSource, station.getTracks().get(4).getTrackUrn(), 0);
    }

    @Test
    public void playStationShouldStartFromZeroOnFirstPlay() {
        final Urn stationUrn = Urn.forTrackStation(123L);
        final StationRecord station = StationFixtures.getStation(stationUrn);
        final PlaySessionSource playSessionSource = PlaySessionSource.forStation(screen.get(), stationUrn, STATIONS);
        final Observable<StationRecord> stationObservable = Observable.just(station);

        presenter.playStation(context(), stationObservable, STATIONS);

        verify(playbackInitiator).playStation(stationUrn, station.getTracks(),
                                              playSessionSource, Urn.NOT_SET, 0);
    }

}
