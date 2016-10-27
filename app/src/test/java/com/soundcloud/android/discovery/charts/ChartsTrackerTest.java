package com.soundcloud.android.discovery.charts;

import static com.soundcloud.android.events.EventQueue.TRACKING;
import static com.soundcloud.android.events.ScreenEvent.create;
import static com.soundcloud.android.main.Screen.AUDIO_TOP_50;
import static com.soundcloud.android.main.Screen.AUDIO_TRENDING;
import static com.soundcloud.android.main.Screen.MUSIC_TOP_50;
import static com.soundcloud.android.main.Screen.MUSIC_TRENDING;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

import com.soundcloud.android.api.model.ChartCategory;
import com.soundcloud.android.api.model.ChartType;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.EventBus;
import com.soundcloud.rx.eventbus.Queue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;

public class ChartsTrackerTest extends AndroidUnitTest {
    private final Urn CHART_GENRE_URN = new Urn("soundcloud:charts:rock");
    private final ChartCategory CHART_CATEGORY = ChartCategory.MUSIC;
    private final ChartType CHART_TYPE = ChartType.TOP;
    private final Urn QUERY_URN = new Urn("soundcloud:chart:1234");

    @Mock private EventBus eventBus;
    @Captor private ArgumentCaptor<Queue<TrackingEvent>> queueCaptor;
    @Captor private ArgumentCaptor<ScreenEvent> screenEventCaptor;
    private ChartsTracker chartsTracker;

    @Before
    public void setUp() throws Exception {
        chartsTracker = new ChartsTracker(eventBus);
        doNothing().when(eventBus).publish(queueCaptor.capture(), screenEventCaptor.capture());
    }

    @Test
    public void doesNotTrackPageViewBeforeDataLoaded() {
        chartsTracker.chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, CHART_TYPE);

        verifyZeroInteractions(eventBus);
    }

    @Test
    public void tracksPageViewOfLoadedPage() {
        chartsTracker.chartDataLoaded(QUERY_URN, CHART_TYPE, CHART_CATEGORY, CHART_GENRE_URN);
        verifyZeroInteractions(eventBus);

        chartsTracker.chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, CHART_TYPE);

        assertEventPostedOnBus(QUERY_URN, MUSIC_TOP_50);
        verify(eventBus).publish(any(Queue.class), any(ScreenEvent.class));
    }

    @Test
    public void tracksSelectedPageViewAfterLoadingFinished() {
        chartsTracker.chartPageSelected(CHART_GENRE_URN, CHART_CATEGORY, CHART_TYPE);
        verifyZeroInteractions(eventBus);

        chartsTracker.chartDataLoaded(QUERY_URN, CHART_TYPE, CHART_CATEGORY, CHART_GENRE_URN);
        assertEventPostedOnBus(QUERY_URN, MUSIC_TOP_50);

        verify(eventBus).publish(any(Queue.class), any(ScreenEvent.class));
    }

    @Test
    public void tracksMusicTop50Screen() {
        final ChartCategory chartCategory = ChartCategory.MUSIC;
        final ChartType chartType = ChartType.TOP;
        chartsTracker.chartDataLoaded(QUERY_URN, chartType, chartCategory, CHART_GENRE_URN);

        chartsTracker.chartPageSelected(CHART_GENRE_URN, chartCategory, chartType);

        assertEventPostedOnBus(QUERY_URN, MUSIC_TOP_50);
        verify(eventBus).publish(any(Queue.class), any(ScreenEvent.class));
    }

    @Test
    public void tracksAudioTop50Screen() {
        final ChartCategory chartCategory = ChartCategory.AUDIO;
        final ChartType chartType = ChartType.TOP;
        chartsTracker.chartDataLoaded(QUERY_URN, chartType, chartCategory, CHART_GENRE_URN);

        chartsTracker.chartPageSelected(CHART_GENRE_URN, chartCategory, chartType);

        assertEventPostedOnBus(QUERY_URN, AUDIO_TOP_50);
        verify(eventBus).publish(any(Queue.class), any(ScreenEvent.class));
    }

    @Test
    public void tracksMusicTrendingScreen() {
        final ChartCategory chartCategory = ChartCategory.MUSIC;
        final ChartType chartType = ChartType.TRENDING;
        chartsTracker.chartDataLoaded(QUERY_URN, chartType, chartCategory, CHART_GENRE_URN);

        chartsTracker.chartPageSelected(CHART_GENRE_URN, chartCategory, chartType);

        assertEventPostedOnBus(QUERY_URN, MUSIC_TRENDING);
        verify(eventBus).publish(any(Queue.class), any(ScreenEvent.class));
    }

    @Test
    public void tracksAudioTrendingScreen() {
        final ChartCategory chartCategory = ChartCategory.AUDIO;
        final ChartType chartType = ChartType.TRENDING;
        chartsTracker.chartDataLoaded(QUERY_URN, chartType, chartCategory, CHART_GENRE_URN);

        chartsTracker.chartPageSelected(CHART_GENRE_URN, chartCategory, chartType);

        assertEventPostedOnBus(QUERY_URN, AUDIO_TRENDING);
        verify(eventBus).publish(any(Queue.class), any(ScreenEvent.class));
    }

    private void assertEventPostedOnBus(final Urn queryUrn, final Screen expectedScreen) {
        String screenTag = create(format(expectedScreen.get(), CHART_GENRE_URN.getStringId()))
                .getScreenTag();
        ScreenEvent screenEvent = screenEventCaptor.getValue();
        assertThat(screenEvent.getScreenTag()).isEqualTo(screenTag);
        assertThat(screenEvent.getQueryUrn()).isEqualTo(queryUrn.toString());
        assertThat(queueCaptor.getValue()).isEqualTo(TRACKING);
    }
}
