package com.soundcloud.android.ads;

import static com.pivotallabs.greatexpectations.Expect.expect;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.LeaveBehindEvent;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestObserver;
import rx.subjects.Subject;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class LeaveBehindImpressionControllerTest {

    private final LeaveBehindEvent LEAVE_BEHIND_SHOWN = LeaveBehindEvent.shown();
    private final LeaveBehindEvent LEAVE_BEHIND_HIDDEN = LeaveBehindEvent.hidden();
    private final CurrentPlayQueueTrackEvent CURRENT_TRACK_CHANGED = CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L));
    private final PlayerUIEvent PLAYER_EXPANDED = PlayerUIEvent.fromPlayerExpanded();
    private final PlayerUIEvent PLAYER_COLLAPSED = PlayerUIEvent.fromPlayerCollapsed();
    private final ActivityLifeCycleEvent ACTIVITY_RESUMED = ActivityLifeCycleEvent.forOnResume(Activity.class);
    private final ActivityLifeCycleEvent ACTIVITY_PAUSED = ActivityLifeCycleEvent.forOnPause(Activity.class);

    @Mock private Activity activity;
    private TestEventBus eventBus;

    private LeaveBehindImpressionController controller;
    private TestObserver<TrackingEvent> observer;

    private Subject<LeaveBehindEvent, LeaveBehindEvent> leaveBehindEventQueue;
    private Subject<CurrentPlayQueueTrackEvent, CurrentPlayQueueTrackEvent> currentTrackQueue;
    private Subject<PlayerUIEvent, PlayerUIEvent> playerUiQueue;
    private Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activitiesLifeCycleQueue;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        controller = new LeaveBehindImpressionController(eventBus);

        leaveBehindEventQueue = eventBus.queue(EventQueue.LEAVE_BEHIND);
        activitiesLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        currentTrackQueue = eventBus.queue(EventQueue.PLAY_QUEUE_TRACK);
        playerUiQueue = eventBus.queue(EventQueue.PLAYER_UI);

        observer = new TestObserver<>();

        controller.trackImpression().subscribe(observer);
    }

    @Test
    public void shouldEmitImpressionWhenLeaveBehindIsShownAndAppIsForegroundAndPlayerIsExpanded() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED);

        expect(observer.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void shouldEmitImpressionOnlyOnce() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED);

        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSED);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);

        expect(observer.getOnNextEvents().size()).toEqual(1);
    }

    @Test
    public void shouldNotEmitImpressionWhenLeaveBehindIsHidden() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_HIDDEN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldNotEmitImpressionWhenTheAppIsInBackground() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        playerUiQueue.onNext(PLAYER_EXPANDED);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldNotEmitImpressionWhenThePlayerIsCollapsed() {
        playerUiQueue.onNext(PLAYER_COLLAPSED);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

}