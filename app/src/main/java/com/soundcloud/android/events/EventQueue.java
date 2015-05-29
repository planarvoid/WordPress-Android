package com.soundcloud.android.events;

import static com.soundcloud.android.playback.service.Playa.StateTransition;

import com.soundcloud.android.rx.eventbus.Queue;
import com.soundcloud.android.sync.SyncResult;

public final class EventQueue {
    public static final Queue<StateTransition> PLAYBACK_STATE_CHANGED = Queue.of(StateTransition.class).replay(StateTransition.DEFAULT).get();
    public static final Queue<PlaybackProgressEvent> PLAYBACK_PROGRESS = Queue.of(PlaybackProgressEvent.class).get();
    public static final Queue<ActivityLifeCycleEvent> ACTIVITY_LIFE_CYCLE = Queue.of(ActivityLifeCycleEvent.class).get();
    public static final Queue<PlayerLifeCycleEvent> PLAYER_LIFE_CYCLE = Queue.of(PlayerLifeCycleEvent.class).get();
    public static final Queue<CurrentUserChangedEvent> CURRENT_USER_CHANGED = Queue.of(CurrentUserChangedEvent.class).get();
    public static final Queue<PlayerUIEvent> PLAYER_UI = Queue.of(PlayerUIEvent.class).replay().get();
    public static final Queue<PlayerUICommand> PLAYER_COMMAND = Queue.of(PlayerUICommand.class).get();
    public static final Queue<PlayQueueEvent> PLAY_QUEUE = Queue.of(PlayQueueEvent.class).get();
    public static final Queue<CurrentPlayQueueTrackEvent> PLAY_QUEUE_TRACK = Queue.of(CurrentPlayQueueTrackEvent.class).replay().get();
    public static final Queue<AdOverlayEvent> AD_OVERLAY = Queue.of(AdOverlayEvent.class).replay().get();
    public static final Queue<SyncResult> SYNC_RESULT = Queue.of(SyncResult.class).replay().get();
    public static final Queue<EntityStateChangedEvent> ENTITY_STATE_CHANGED = Queue.of(EntityStateChangedEvent.class).get();
    public static final Queue<CurrentDownloadEvent> CURRENT_DOWNLOAD = Queue.of(CurrentDownloadEvent.class).replay(CurrentDownloadEvent.idle()).get();
    public static final Queue<UploadEvent> UPLOAD = Queue.of(UploadEvent.class).replay(UploadEvent.idle()).get();

    // tracking event queues
    public static final Queue<TrackingEvent> TRACKING = Queue.of(TrackingEvent.class).get();
    public static final Queue<PlaybackPerformanceEvent> PLAYBACK_PERFORMANCE = Queue.of(PlaybackPerformanceEvent.class).get();
    public static final Queue<PlaybackErrorEvent> PLAYBACK_ERROR = Queue.of(PlaybackErrorEvent.class).get();
    public static final Queue<OnboardingEvent> ONBOARDING = Queue.of(OnboardingEvent.class).get();
}
