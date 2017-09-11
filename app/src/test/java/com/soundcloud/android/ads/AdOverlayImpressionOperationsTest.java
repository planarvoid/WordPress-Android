package com.soundcloud.android.ads;

import static org.mockito.Mockito.mock;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import io.reactivex.observers.TestObserver;
import io.reactivex.subjects.Subject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;

public class AdOverlayImpressionOperationsTest extends AndroidUnitTest {

    private static final LeaveBehindAd AD_META_DATA = AdFixtures.getLeaveBehindAd(Urn.forTrack(123));
    private final AdOverlayEvent LEAVE_BEHIND_SHOWN = AdOverlayEvent.shown(Urn.forTrack(123L),
                                                                           AD_META_DATA,
                                                                           new TrackSourceInfo("origin_screen", true));
    private final AdOverlayEvent LEAVE_BEHIND_HIDDEN = AdOverlayEvent.hidden();
    private final PlayerUIEvent PLAYER_EXPANDED = PlayerUIEvent.fromPlayerExpanded();
    private final PlayerUIEvent PLAYER_COLLAPSED = PlayerUIEvent.fromPlayerCollapsed();
    private final TestEventBusV2 eventBus = new TestEventBusV2();

    @Mock private Activity activity;
    @Mock private AccountOperations accountOperations;
    private ActivityLifeCycleEvent activityResumed;
    private ActivityLifeCycleEvent activityPaused;

    private Subject<AdOverlayEvent> leaveBehindEventQueue;
    private Subject<PlayerUIEvent> playerUiQueue;
    private Subject<ActivityLifeCycleEvent> activitiesLifeCycleQueue;


    @Before
    public void setUp() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        AdOverlayImpressionOperations controller = new AdOverlayImpressionOperations(eventBus, accountOperations);

        activityResumed = ActivityLifeCycleEvent.forOnResume(activity);
        activityPaused = ActivityLifeCycleEvent.forOnPause(activity);

        leaveBehindEventQueue = eventBus.queue(EventQueue.AD_OVERLAY);
        activitiesLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        playerUiQueue = eventBus.queue(EventQueue.PLAYER_UI);

        PlayerAdsControllerProxy listener = new PlayerAdsControllerProxy(eventBus, () -> mock(PlayerAdsController.class), () -> mock(VisualAdImpressionOperations.class), () -> controller);
        listener.subscribe();
    }

    @Test
    public void emitImpressionWhenLeaveBehindIsShownAndAppIsForegroundAndPlayerIsExpanded() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(activityResumed);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        observer.assertValueCount(1);
    }

    @Test
    public void doNotRepeatImpressionWhenActivityLifeCycleStateChange() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        activitiesLifeCycleQueue.onNext(activityResumed);
        playerUiQueue.onNext(PLAYER_EXPANDED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        activitiesLifeCycleQueue.onNext(activityPaused);
        activitiesLifeCycleQueue.onNext(activityResumed);

        observer.assertValueCount(1);
    }

    @Test
    public void doNotRepeatImpressionWhenExpendedStateChange() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(activityResumed);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        playerUiQueue.onNext(PLAYER_COLLAPSED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        observer.assertValueCount(1);
    }

    @Test
    public void emit2ImpressionsWhenTheLeaveBehindIsShowTwice() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(activityResumed);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_HIDDEN);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        observer.assertValueCount(2);
    }

    @Test
    public void doNotRepeatImpressionWhenLeaveBehindWhenLeaveBehindShownStatesWithoutHidden() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(activityResumed);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        observer.assertValueCount(1);
    }

    @Test
    public void doNotEmitImpressionWhenLeaveBehindIsHidden() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_HIDDEN);
        activitiesLifeCycleQueue.onNext(activityResumed);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        observer.assertValueCount(0);
    }

    @Test
    public void doNotEmitImpressionWhenTheAppIsInBackground() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        activitiesLifeCycleQueue.onNext(activityPaused);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        observer.assertValueCount(0);
    }

    @Test
    public void doNotEmitImpressionWhenThePlayerIsCollapsed() {
        TestObserver observer = eventBus.queue(EventQueue.TRACKING).test();

        playerUiQueue.onNext(PLAYER_COLLAPSED);
        activitiesLifeCycleQueue.onNext(activityResumed);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        observer.assertValueCount(0);
    }

}
