package com.soundcloud.android.events;

import static com.soundcloud.android.playback.Player.StateTransition;

import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.rx.eventbus.Queue;
import rx.functions.Action1;

public final class EventQueue {

    private static final Action1<Throwable> ON_ERROR = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            ErrorUtils.handleThrowable(throwable, getClass());
        }
    };

    public static final Queue<StateTransition> PLAYBACK_STATE_CHANGED = Queue.of(StateTransition.class).onError(ON_ERROR).replay(StateTransition.DEFAULT).get();
    public static final Queue<PlaybackProgressEvent> PLAYBACK_PROGRESS = Queue.of(PlaybackProgressEvent.class).onError(ON_ERROR).get();
    public static final Queue<ActivityLifeCycleEvent> ACTIVITY_LIFE_CYCLE = Queue.of(ActivityLifeCycleEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlayerLifeCycleEvent> PLAYER_LIFE_CYCLE = Queue.of(PlayerLifeCycleEvent.class).onError(ON_ERROR).replay(PlayerLifeCycleEvent.forDestroyed()).get();
    public static final Queue<CurrentUserChangedEvent> CURRENT_USER_CHANGED = Queue.of(CurrentUserChangedEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlayerUIEvent> PLAYER_UI = Queue.of(PlayerUIEvent.class).onError(ON_ERROR).replay().get();
    public static final Queue<PlayerUICommand> PLAYER_COMMAND = Queue.of(PlayerUICommand.class).onError(ON_ERROR).get();
    public static final Queue<PlayQueueEvent> PLAY_QUEUE = Queue.of(PlayQueueEvent.class).onError(ON_ERROR).get();
    public static final Queue<CurrentPlayQueueTrackEvent> PLAY_QUEUE_TRACK = Queue.of(CurrentPlayQueueTrackEvent.class).onError(ON_ERROR).replay().get();
    public static final Queue<AdOverlayEvent> AD_OVERLAY = Queue.of(AdOverlayEvent.class).onError(ON_ERROR).replay().get();
    public static final Queue<SyncResult> SYNC_RESULT = Queue.of(SyncResult.class).onError(ON_ERROR).replay().get();
    public static final Queue<EntityStateChangedEvent> ENTITY_STATE_CHANGED = Queue.of(EntityStateChangedEvent.class).onError(ON_ERROR).get();
    public static final Queue<CurrentDownloadEvent> CURRENT_DOWNLOAD = Queue.of(CurrentDownloadEvent.class).onError(ON_ERROR).replay(CurrentDownloadEvent.idle()).get();
    public static final Queue<UploadEvent> UPLOAD = Queue.of(UploadEvent.class).onError(ON_ERROR).replay(UploadEvent.idle()).get();
    public static final Queue<PolicyUpdateEvent> POLICY_UPDATES = Queue.of(PolicyUpdateEvent.class).onError(ON_ERROR).get();


    // tracking event queues
    public static final Queue<TrackingEvent> TRACKING = Queue.of(TrackingEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlaybackPerformanceEvent> PLAYBACK_PERFORMANCE = Queue.of(PlaybackPerformanceEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlaybackErrorEvent> PLAYBACK_ERROR = Queue.of(PlaybackErrorEvent.class).onError(ON_ERROR).get();
    public static final Queue<OnboardingEvent> ONBOARDING = Queue.of(OnboardingEvent.class).onError(ON_ERROR).get();
}
