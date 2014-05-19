package com.soundcloud.android.events;

import static com.soundcloud.android.events.EventBus.QueueDescriptor;
import static com.soundcloud.android.events.EventBus.QueueDescriptor.create;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

public final class EventQueue {
    public static final QueueDescriptor<StateTransition> PLAYBACK_STATE_CHANGED = create(StateTransition.class, StateTransition.DEFAULT);
    public static final QueueDescriptor<PlaybackProgressEvent> PLAYBACK_PROGRESS = create(PlaybackProgressEvent.class);
    public static final QueueDescriptor<PlayableChangedEvent> PLAYABLE_CHANGED = create(PlayableChangedEvent.class);
    public static final QueueDescriptor<UIEvent> UI = create(UIEvent.class);
    public static final QueueDescriptor<ActivityLifeCycleEvent> ACTIVITY_LIFE_CYCLE = create(ActivityLifeCycleEvent.class);
    public static final QueueDescriptor<PlayerLifeCycleEvent> PLAYER_LIFE_CYCLE = create(PlayerLifeCycleEvent.class);
    public static final QueueDescriptor<String> SCREEN_ENTERED = create("screen", String.class);
    public static final QueueDescriptor<CurrentUserChangedEvent> CURRENT_USER_CHANGED = create(CurrentUserChangedEvent.class);
    public static final QueueDescriptor<OnboardingEvent> ONBOARDING = create(OnboardingEvent.class);
    public static final QueueDescriptor<SearchEvent> SEARCH = create(SearchEvent.class);
    public static final QueueDescriptor<PlayControlEvent> PLAY_CONTROL = create(PlayControlEvent.class);
    public static final QueueDescriptor<PlaybackSessionEvent> PLAYBACK_SESSION = create(PlaybackSessionEvent.class);
    public static final QueueDescriptor<PlaybackPerformanceEvent> PLAYBACK_PERFORMANCE = create(PlaybackPerformanceEvent.class);
    public static final QueueDescriptor<PlaybackErrorEvent> PLAYBACK_ERROR = create(PlaybackErrorEvent.class);
    public static final QueueDescriptor<PlayerUIEvent> PLAYER_UI = create(PlayerUIEvent.class);
    public static final QueueDescriptor<PlayQueueEvent> PLAY_QUEUE = create(PlayQueueEvent.class);
}
