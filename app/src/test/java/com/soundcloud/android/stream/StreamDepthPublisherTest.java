package com.soundcloud.android.stream;

import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.ScrollDepthEvent;
import com.soundcloud.android.events.ScrollDepthEvent.Action;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.rx.eventbus.TestEventBus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class StreamDepthPublisherTest extends AndroidUnitTest {

    @Mock StaggeredGridLayoutManager layoutManager;
    @Mock RecyclerView recyclerView;

    private TestEventBus eventBus = new TestEventBus();
    private StreamDepthPublisher publisher;

    @Before
    public void setUp() {
        publisher = new StreamDepthPublisher(layoutManager, false, eventBus);

        setEdgeVisiblePosition(1, 3);
    }

    @Test
    public void onTabFocusChangeTrueEmitsStartEventIfNoPreviousEvents() {
        publisher.onFocusChange(true);

        final TrackingEvent trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(trackingEvent).isInstanceOf(ScrollDepthEvent.class);
        assertThat(((ScrollDepthEvent) trackingEvent).action()).isEqualTo(Action.START);
    }

    @Test
    public void onTabFocusChangeTrueThenFalseEmitsStartEventAndEndEvent() {
        publisher.onFocusChange(true);
        publisher.onFocusChange(false);

        final List<Action> actionsEmitted = Lists.transform(eventBus.eventsOn(EventQueue.TRACKING),
                                                            trackingEventsToActions());
        assertThat(actionsEmitted).contains(Action.START, Action.END);
    }

    @Test
    public void onTabFocusChangeFalseEmitsNothingIfNoStartEventPrior() {
        publisher.onFocusChange(false);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void twoEndEventsWillNotBeEmittedInARow() {
        publisher.onFocusChange(true);
        publisher.onFocusChange(false);
        publisher.onFocusChange(false);

        final List<Action> actionsEmitted = Lists.transform(eventBus.eventsOn(EventQueue.TRACKING),
                                                            trackingEventsToActions());
        assertThat(actionsEmitted).contains(Action.START, Action.END);
    }

    @Test
    public void playerVisibleCausesEndEventToEmitIfStartEventExists() {
        publisher.onFocusChange(true);

        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        final List<Action> actionsEmitted = Lists.transform(eventBus.eventsOn(EventQueue.TRACKING),
                                                            trackingEventsToActions());
        assertThat(actionsEmitted).contains(Action.START, Action.END);
    }

    @Test
    public void playerVisibleDoesNotEmitEndEventIfNoStartEventExists() {
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.fromPlayerExpanded());

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void onScrollEventDragWillNotEmitScrollStartIfNoStartExists() {
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).isEmpty();
    }

    @Test
    public void onScrollEventDragWillEmitScrollStart() {
        publisher.onFocusChange(true);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING);

        final List<Action> actionsEmitted = Lists.transform(eventBus.eventsOn(EventQueue.TRACKING),
                                                            trackingEventsToActions());
        assertThat(actionsEmitted).contains(Action.START, Action.SCROLL_START);
    }

    @Test
    public void onScrollEventIdleWillEmitScrollStop() {
        publisher.onFocusChange(true);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE);

        final List<Action> actionsEmitted = Lists.transform(eventBus.eventsOn(EventQueue.TRACKING),
                                                            trackingEventsToActions());
        assertThat(actionsEmitted).contains(Action.START, Action.SCROLL_START, Action.SCROLL_STOP);
    }

    @Test
    public void onScrollEventsCanHaveMultipleDragsAndIdlesBeforeEnd() {
        publisher.onFocusChange(true);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE);
        publisher.onFocusChange(false);

        final List<Action> actionsEmitted = Lists.transform(eventBus.eventsOn(EventQueue.TRACKING),
                                                            trackingEventsToActions());
        assertThat(actionsEmitted).contains(Action.START, Action.SCROLL_START, Action.SCROLL_STOP,
                                            Action.SCROLL_START, Action.SCROLL_START, Action.SCROLL_STOP,
                                            Action.END);
    }

    @Test
    public void startEventsEmittedDoNotHaveEarliestAndLatestItems() {
        publisher.onFocusChange(true);

        final ScrollDepthEvent event = (ScrollDepthEvent) eventBus.firstEventOn(EventQueue.TRACKING);
        assertThat(event.action()).isEqualTo(Action.START);
        assertThat(event.earliestItems()).isEmpty();
        assertThat(event.latestItems()).isEmpty();
    }

    @Test
    public void scrollStartsEventsEmittedHaveEarliestAndLatestItems() {
        publisher.onFocusChange(true);

        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_DRAGGING);

        final ScrollDepthEvent event = (ScrollDepthEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.action()).isEqualTo(Action.SCROLL_START);
        assertThat(event.earliestItems()).isNotEmpty();
        assertThat(event.latestItems()).isNotEmpty();
    }

    @Test
    public void scrollStopEventsEmittedHaveEarliestAndLatestItems() {
        publisher.onFocusChange(true);
        publisher.onScrollStateChanged(recyclerView, RecyclerView.SCROLL_STATE_IDLE);

        final ScrollDepthEvent event = (ScrollDepthEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.action()).isEqualTo(Action.SCROLL_STOP);
        assertThat(event.earliestItems()).isNotEmpty();
        assertThat(event.latestItems()).isNotEmpty();
    }

    @Test
    public void endEventsEmittedHaveEarliestAndLatestItems() {
        publisher.onFocusChange(true);
        publisher.onFocusChange(false);

        final ScrollDepthEvent event = (ScrollDepthEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.action()).isEqualTo(Action.END);
        assertThat(event.earliestItems()).isNotEmpty();
        assertThat(event.latestItems()).isNotEmpty();
    }

    private void setEdgeVisiblePosition(int firstPosition, int lastPosition) {
        int[] firstEdge = new int[]{firstPosition};
        int[] lastEdge = new int[]{lastPosition};

        when(layoutManager.findFirstVisibleItemPositions(any(int[].class))).thenReturn(firstEdge);
        when(layoutManager.findLastVisibleItemPositions(any(int[].class))).thenReturn(lastEdge);
    }

    private Function<TrackingEvent, Action> trackingEventsToActions() {
        return event -> ((ScrollDepthEvent) event).action();
    }
}
