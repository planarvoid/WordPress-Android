package com.soundcloud.android.events;

import com.soundcloud.android.configuration.ForceUpdateEvent;
import com.soundcloud.android.configuration.UserPlanChangedEvent;
import com.soundcloud.android.offline.OfflineContentChangedEvent;
import com.soundcloud.android.playback.PlaybackStateTransition;
import com.soundcloud.android.sync.SyncResult;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.rx.eventbus.Queue;
import rx.functions.Action1;

public final class EventQueue {

    private static final Action1<Throwable> ON_ERROR = new Action1<Throwable>() {
        @Override
        public void call(Throwable throwable) {
            ErrorUtils.handleThrowable(throwable, EventQueue.class);
        }
    };

    // playback
    public static final Queue<PlaybackStateTransition> PLAYBACK_STATE_CHANGED = Queue.of(PlaybackStateTransition.class).onError(ON_ERROR).replay(PlaybackStateTransition.DEFAULT).get();
    public static final Queue<PlayerLifeCycleEvent> PLAYER_LIFE_CYCLE = Queue.of(PlayerLifeCycleEvent.class).onError(ON_ERROR).replay(PlayerLifeCycleEvent.forDestroyed()).get();
    public static final Queue<PlaybackProgressEvent> PLAYBACK_PROGRESS = Queue.of(PlaybackProgressEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlayerUIEvent> PLAYER_UI = Queue.of(PlayerUIEvent.class).onError(ON_ERROR).replay(PlayerUIEvent.fromPlayerCollapsed()).get();
    public static final Queue<PlayerUICommand> PLAYER_COMMAND = Queue.of(PlayerUICommand.class).onError(ON_ERROR).get();
    public static final Queue<PlayQueueEvent> PLAY_QUEUE = Queue.of(PlayQueueEvent.class).onError(ON_ERROR).get();
    public static final Queue<CurrentPlayQueueItemEvent> CURRENT_PLAY_QUEUE_ITEM = Queue.of(CurrentPlayQueueItemEvent.class).onError(ON_ERROR).replay().get();
    public static final Queue<AdOverlayEvent> AD_OVERLAY = Queue.of(AdOverlayEvent.class).onError(ON_ERROR).replay().get();

    // accounts + users
    public static final Queue<CurrentUserChangedEvent> CURRENT_USER_CHANGED = Queue.of(CurrentUserChangedEvent.class).onError(ON_ERROR).get();
    public static final Queue<UserPlanChangedEvent> USER_PLAN_CHANGE = Queue.of(UserPlanChangedEvent.class).onError(ON_ERROR).get();
    public static final Queue<ForceUpdateEvent> FORCE_UPDATE = Queue.of(ForceUpdateEvent.class).replay().onError(ON_ERROR).get();

    // application state
    public static final Queue<ConnectionType> NETWORK_CONNECTION_CHANGED = Queue.of(ConnectionType.class).onError(ON_ERROR).replay(ConnectionType.UNKNOWN).get();
    public static final Queue<ActivityLifeCycleEvent> ACTIVITY_LIFE_CYCLE = Queue.of(ActivityLifeCycleEvent.class).onError(ON_ERROR).get();

    // data
    public static final Queue<SyncResult> SYNC_RESULT = Queue.of(SyncResult.class).onError(ON_ERROR).replay().get();
    public static final Queue<EntityStateChangedEvent> ENTITY_STATE_CHANGED = Queue.of(EntityStateChangedEvent.class).onError(ON_ERROR).get();
    public static final Queue<OfflineContentChangedEvent> OFFLINE_CONTENT_CHANGED = Queue.of(OfflineContentChangedEvent.class).onError(ON_ERROR).replay().get();
    public static final Queue<UploadEvent> UPLOAD = Queue.of(UploadEvent.class).onError(ON_ERROR).replay(UploadEvent.idle()).get();
    public static final Queue<PolicyUpdateEvent> POLICY_UPDATES = Queue.of(PolicyUpdateEvent.class).onError(ON_ERROR).get();
    public static final Queue<StreamEvent> STREAM = Queue.of(StreamEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlayHistoryEvent> PLAY_HISTORY = Queue.of(PlayHistoryEvent.class).onError(ON_ERROR).get();

    // tracking event queues
    public static final Queue<TrackingEvent> TRACKING = Queue.of(TrackingEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlaybackPerformanceEvent> PLAYBACK_PERFORMANCE = Queue.of(PlaybackPerformanceEvent.class).onError(ON_ERROR).get();
    public static final Queue<PlaybackErrorEvent> PLAYBACK_ERROR = Queue.of(PlaybackErrorEvent.class).onError(ON_ERROR).get();
    public static final Queue<OnboardingEvent> ONBOARDING = Queue.of(OnboardingEvent.class).onError(ON_ERROR).get();
}
