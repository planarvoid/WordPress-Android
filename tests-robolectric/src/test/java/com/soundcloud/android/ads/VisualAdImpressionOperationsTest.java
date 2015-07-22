package com.soundcloud.android.ads;

import static com.pivotallabs.greatexpectations.Expect.expect;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.observers.TestObserver;
import rx.subjects.Subject;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class VisualAdImpressionOperationsTest {
    private final CurrentPlayQueueTrackEvent CURRENT_TRACK_CHANGED_EVENT = CurrentPlayQueueTrackEvent.fromPositionChanged(Urn.forTrack(123L));
    private final PlayerUIEvent PLAYER_EXPANDED_EVENT = PlayerUIEvent.fromPlayerExpanded();
    private final PlayerUIEvent PLAYER_COLLAPSED_EVENT = PlayerUIEvent.fromPlayerCollapsed();
    private final ActivityLifeCycleEvent ACTIVITY_RESUME_EVENT = ActivityLifeCycleEvent.forOnResume(Activity.class);
    private final ActivityLifeCycleEvent ACTIVITY_PAUSE_EVENT = ActivityLifeCycleEvent.forOnPause(Activity.class);

    @Mock private PlayQueueManager playQueueManager;
    @Mock private AccountOperations accountOperations;
    @Mock private Activity activity;
    @Mock private AdsOperations adsOperations;
    private TestEventBus eventBus;

    private VisualAdImpressionOperations controller;
    private TestObserver<TrackingEvent> observer;

    private Subject<CurrentPlayQueueTrackEvent, CurrentPlayQueueTrackEvent> currentTrackQueue;
    private Subject<PlayerUIEvent, PlayerUIEvent> playerUiQueue;
    private Subject<ActivityLifeCycleEvent, ActivityLifeCycleEvent> activitiesLifeCycleQueue;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        controller = new VisualAdImpressionOperations(eventBus, playQueueManager, accountOperations, adsOperations);
        activitiesLifeCycleQueue = eventBus.queue(EventQueue.ACTIVITY_LIFE_CYCLE);
        currentTrackQueue = eventBus.queue(EventQueue.PLAY_QUEUE_TRACK);
        playerUiQueue = eventBus.queue(EventQueue.PLAYER_UI);
        observer = new TestObserver<>();

        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(true);
        when(playQueueManager.getCurrentMetaData()).thenReturn(TestPropertySets.audioAdProperties(Urn.forTrack(123L)));
        when(playQueueManager.getCurrentTrackSourceInfo()).thenReturn(new TrackSourceInfo("origin screen", true));
        when(accountOperations.getLoggedInUserUrn()).thenReturn(Urn.forUser(42L));

        controller.trackImpression().subscribe(observer);
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
        when(adsOperations.isCurrentTrackAudioAd()).thenReturn(false);
        when(playQueueManager.getCurrentMetaData()).thenReturn(PropertySet.create());

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
