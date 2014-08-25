package com.soundcloud.android.ads;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.TestPropertySets;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AudioAdCompanionImpressionEvent;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.service.PlayQueueManager;
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
public class AdCompanionImpressionControllerTest {
    private final CurrentPlayQueueTrackEvent CURRENT_TRACK_CHANGED_EVENT = CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L));
    private final PlayerUIEvent PLAYER_EXPANDED_EVENT = PlayerUIEvent.fromPlayerExpanded();
    private final PlayerUIEvent PLAYER_COLLAPSED_EVENT = PlayerUIEvent.fromPlayerCollapsed();
    private final ActivityLifeCycleEvent ACTIVITY_RESUME_EVENT = ActivityLifeCycleEvent.forOnResume(Activity.class);
    private final ActivityLifeCycleEvent ACTIVITY_PAUSE_EVENT = ActivityLifeCycleEvent.forOnPause(Activity.class);

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private Activity activity;
    private TestEventBus eventBus;

    private AdCompanionImpressionController controller;
    private TestObserver<AudioAdCompanionImpressionEvent> observer;

    private Subject<CurrentPlayQueueTrackEvent, CurrentPlayQueueTrackEvent> currentTrackQueue;
    private Subject<PlayerUIEvent, PlayerUIEvent> playerUiQueue;
    private Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activitiesLifeCycleQueue;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        controller = new AdCompanionImpressionController(eventBus, playQueueManager, accountOperations);
        activitiesLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        currentTrackQueue = eventBus.queue(EventQueue.PLAY_QUEUE_TRACK);
        playerUiQueue = eventBus.queue(EventQueue.PLAYER_UI);
        observer = new TestObserver<AudioAdCompanionImpressionEvent>();

        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getAudioAd()).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(123L)));

        controller.companionImpressionEvent().subscribe(observer);
    }

    @Test
    public void shouldLogWhenAppIsForegroundAndCurrentTrackIsAnAdAndPlayerIsExpanded() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUME_EVENT);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        expect(observer.getOnNextEvents()).toNumber(1);
    }

    @Test
    public void shouldLogOnlyOnceTheCurrentAd() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUME_EVENT);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSE_EVENT);
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUME_EVENT);

        expect(observer.getOnNextEvents().size()).toEqual(1);
    }

    @Test
    public void shouldNotLogWhenTheCurrentTrackIsNotAnAd() {
        when(playQueueManager.isCurrentTrackAudioAd()).thenReturn(false);
        when(playQueueManager.getAudioAd()).thenReturn(null);

        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUME_EVENT);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldNotLogWhenTheAppIsInBackground() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_PAUSE_EVENT);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldNotLogWhenThePlayerIsCollapsed() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUME_EVENT);
        playerUiQueue.onNext(PLAYER_COLLAPSED_EVENT);
        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }

    @Test
    public void shouldLogWhenTheTrackChanged() {
        activitiesLifeCycleQueue.onNext(ACTIVITY_RESUME_EVENT);
        playerUiQueue.onNext(PLAYER_EXPANDED_EVENT);
        expect(observer.getOnNextEvents()).toBeEmpty();

        currentTrackQueue.onNext(CURRENT_TRACK_CHANGED_EVENT);
        expect(observer.getOnNextEvents()).toNumber(1);
    }
}