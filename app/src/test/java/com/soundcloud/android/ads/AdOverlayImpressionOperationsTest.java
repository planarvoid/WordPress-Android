package com.soundcloud.android.ads;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AdOverlayEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestObserver;
import rx.subjects.Subject;

import android.app.Activity;

public class AdOverlayImpressionOperationsTest extends AndroidUnitTest {

    private static final PropertySet AD_META_DATA = TestPropertySets.leaveBehindForPlayer();
    private final AdOverlayEvent LEAVE_BEHIND_SHOWN = AdOverlayEvent.shown(Urn.forTrack(123L), AD_META_DATA, new TrackSourceInfo("origin_screen", true));
    private final AdOverlayEvent LEAVE_BEHIND_HIDDEN = AdOverlayEvent.hidden();
    private final PlayerUIEvent PLAYER_EXPANDED = PlayerUIEvent.fromPlayerExpanded();
    private final PlayerUIEvent PLAYER_COLLAPSED = PlayerUIEvent.fromPlayerCollapsed();
    private final ActivityLifeCycleEvent ACTIVITY_RESUMED = ActivityLifeCycleEvent.forOnResume(Activity.class);
    private final ActivityLifeCycleEvent ACTIVITY_PAUSED = ActivityLifeCycleEvent.forOnPause(Activity.class);

    @Mock private Activity activity;
    @Mock private AccountOperations accountOperations;
    private TestEventBus eventBus;

    private AdOverlayImpressionOperations controller;
    private TestObserver<TrackingEvent> observer;

    private Subject<AdOverlayEvent, AdOverlayEvent> leaveBehindEventQueue;
    private Subject<PlayerUIEvent, PlayerUIEvent> playerUiQueue;
    private Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activitiesLifeCycleQueue;

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(456L));
        eventBus = new TestEventBus();
        controller = new AdOverlayImpressionOperations(eventBus, accountOperations);

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

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void doNotRepeatImpressionWhenActivityLifeCycleStateChange() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSED);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void doNotRepeatImpressionWhenExpendedStateChange() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        playerUiQueue.onNext(PLAYER_COLLAPSED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void emit2ImpressionsWhenTheLeaveBehindIsShowTwice() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_HIDDEN);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        assertThat(observer.getOnNextEvents()).hasSize(2);
    }

    @Test
    public void doNotRepeatImpressionWhenLeaveBehindWhenLeaveBehindShownStatesWithoutHidden() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        assertThat(observer.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void doNotEmitImpressionWhenLeaveBehindIsHidden() {
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_HIDDEN);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        assertThat(observer.getOnNextEvents()).isEmpty();
    }

    @Test
    public void doNotEmitImpressionWhenTheAppIsInBackground() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);
        playerUiQueue.onNext(PLAYER_EXPANDED);

        assertThat(observer.getOnNextEvents()).isEmpty();
    }

    @Test
    public void doNotEmitImpressionWhenThePlayerIsCollapsed() {
        playerUiQueue.onNext(PLAYER_COLLAPSED);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUMED);
        leaveBehindEventQueue.onNext(LEAVE_BEHIND_SHOWN);

        assertThat(observer.getOnNextEvents()).isEmpty();
    }

}