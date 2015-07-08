package com.soundcloud.android.ads;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestObserver;
import rx.subjects.Subject;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class AdOverlayImpressionOperationsTest {

    private final AdOverlayEvent LEAVE_BEHIND_SHOWN = AdOverlayEvent.shown();
    private final AdOverlayEvent LEAVE_BEHIND_HIDDEN = AdOverlayEvent.hidden();
    private final PlayerUIEvent PLAYER_EXPANDED = PlayerUIEvent.fromPlayerExpanded();
    private final PlayerUIEvent PLAYER_COLLAPSED = PlayerUIEvent.fromPlayerCollapsed();
    private final ActivityLifeCycleEvent ACTIVITY_RESUMED = ActivityLifeCycleEvent.forOnResume(Activity.class);
    private final ActivityLifeCycleEvent ACTIVITY_PAUSED = ActivityLifeCycleEvent.forOnPause(Activity.class);

    @Mock private Activity activity;
    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    private TestEventBus eventBus;

    private AdOverlayImpressionOperations controller;
    private TestObserver<TrackingEvent> observer;

    private Subject<AdOverlayEvent, AdOverlayEvent> leaveBehindEventQueue;
    private Subject<PlayerUIEvent, PlayerUIEvent> playerUiQueue;
    private Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activitiesLifeCycleQueue;

    @Before
    public void setUp() throws Exception {
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.leaveBehindForPlayer());
        when(playQueueManager.getCurrentTrackUrn()).thenReturn(Urn.forTrack(123L));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin_screen", true));
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        eventBus = new TestEventBus();
        controller = new AdOverlayImpressionOperations(eventBus, playQueueManager, accountOperations);

        leaveBehindEventQueue = eventBus.queue(EventQueue.AD_OVERLAY);
        activitiesLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        playerUiQueue = eventBus.queue(EventQueue.PLAYER_UI);

        observer = new TestObserver<>();

        controller.trackImpression().subscribe(observer);
    }

    @Test
    public void emitImpressionWhenLeaveBehindIsShownAndAppIsForegroundAndPlayerIsExpanded() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        expect(observer.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void doNotRepeatImpressionWhenActivityLifeCycleStateChange() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSED);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);

        expect(observer.getOnNextEvents().size()).toEqual(1);
    }

    @Test
    public void doNotRepeatImpressionWhenExpendedStateChange() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        playerUiQueue.onNext(PLAYER_COLLAPSED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        expect(observer.getOnNextEvents().size()).toEqual(1);
    }

    @Test
    public void emit2ImpressionsWhenTheLeaveBehindIsShowTwice() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_HIDDEN);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        expect(observer.getOnNextEvents().size()).toEqual(2);
    }

    @Test
    public void doNotRepeatImpressionWhenLeaveBehindWhenLeaveBehindShownStatesWithoutHidden() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        expect(observer.getOnNextEvents().size()).toEqual(1);
    }

    @Test
    public void doNotEmitImpressionWhenLeaveBehindIsHidden() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_HIDDEN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void doNotEmitImpressionWhenTheAppIsInBackground() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void doNotEmitImpressionWhenThePlayerIsCollapsed() {
        playerUiQueue.onNext(PLAYER_COLLAPSED);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

}