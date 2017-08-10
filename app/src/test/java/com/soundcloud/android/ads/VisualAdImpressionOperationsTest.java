package com.soundcloud.android.ads;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueItem;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPlayQueueItem;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;
import rx.subjects.Subject;

import android.app.Activity;

public class VisualAdImpressionOperationsTest extends AndroidUnitTest {
    private final PlayQueueItem AD_QUEUE_ITEM = TestPlayQueueItem.createAudioAd(AdFixtures.getAudioAd(Urn.forTrack(123L)));
    private final CurrentPlayQueueItemEvent CURRENT_TRACK_CHANGED_EVENT = CurrentPlayQueueItemEvent.fromPositionChanged(
            AD_QUEUE_ITEM,
            Urn.NOT_SET,
            0);
    private final PlayerUIEvent PLAYER_EXPANDED_EVENT = PlayerUIEvent.fromPlayerExpanded();
    private final PlayerUIEvent PLAYER_COLLAPSED_EVENT = PlayerUIEvent.fromPlayerCollapsed();

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private Activity activity;
    private ActivityLifeCycleEvent activityResumeEvent;
    private ActivityLifeCycleEvent activityPauseEvent;

    private TestSubscriber<Object> subscriber;

    private Subject<CurrentPlayQueueItemEvent, CurrentPlayQueueItemEvent> currentTrackQueue;
    private Subject<PlayerUIEvent, PlayerUIEvent> playerUiQueue;
    private Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activitiesLifeCycleQueue;

    @Before
    public void setUp() throws Exception {
        TestEventBus eventBus = new TestEventBus();
        VisualAdImpressionOperations controller = new VisualAdImpressionOperations(eventBus,
                                                                                   playQueueManager,
                                                                                   accountOperations);
        activitiesLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        currentTrackQueue = eventBus.queue(EventQueue.CURRENT_PLAY_QUEUE_ITEM);
        playerUiQueue = eventBus.queue(EventQueue.PLAYER_UI);
        subscriber = new TestSubscriber<>();
        activityResumeEvent = ActivityLifeCycleEvent.forOnResume(activity);
        activityPauseEvent = ActivityLifeCycleEvent.forOnPause(activity);

        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(AD_QUEUE_ITEM);
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(42L));

        controller.trackImpression().subscribe(subscriber);
    }

    @Test
    public void shouldLogWhenAppIsForegroundAndCurrentTrackIsAnAdAndPlayerIsExpanded() {
        activitiesLifeCycleQueue.onNext(activityResumeEvent);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void shouldLogOnlyOnceTheCurrentAd() {
        activitiesLifeCycleQueue.onNext(activityResumeEvent);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        activitiesLifeCycleQueue.onNext(activityPauseEvent);
        activitiesLifeCycleQueue.onNext(activityResumeEvent);

        assertThat(subscriber.getOnNextEvents()).hasSize(1);
    }

    @Test
    public void shouldNotLogWhenTheCurrentTrackIsNotAnAd() {
        final PlayQueueItem nonAdPlayQueueItem = TestPlayQueueItem.createTrack(Urn.forTrack(123L));
        when(playQueueManager.getCurrentPlayQueueItem()).thenReturn(nonAdPlayQueueItem);

        final CurrentPlayQueueItemEvent playQueueItemEvent = CurrentPlayQueueItemEvent.fromPositionChanged(
                nonAdPlayQueueItem,
                Urn.NOT_SET,
                0);

        activitiesLifeCycleQueue.onNext(activityResumeEvent);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(playQueueItemEvent);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void shouldNotLogWhenTheAppIsInBackground() {
        activitiesLifeCycleQueue.onNext(activityPauseEvent);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void shouldNotLogWhenThePlayerIsCollapsed() {
        activitiesLifeCycleQueue.onNext(activityResumeEvent);
        playerUiQueue.onNext(PLAYER_COLLAPSED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        assertThat(subscriber.getOnNextEvents()).isEmpty();
    }

    @Test
    public void shouldLogWhenTheTrackChanged() {
        activitiesLifeCycleQueue.onNext(activityResumeEvent);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        assertThat(subscriber.getOnNextEvents()).isEmpty();

        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);
        assertThat(subscriber.getOnNextEvents()).hasSize(1);
    }
}
