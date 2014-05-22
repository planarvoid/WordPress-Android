package com.soundcloud.android.events;

import static com.soundcloud.android.events.EventBus.Queue;
import static com.soundcloud.android.events.EventBus.Queue.create;
import static com.soundcloud.android.playback.service.Playa.StateTransition;

public final class EventQueue {
    public static final Queue<StateTransition> PLAYBACK_STATE_CHANGED = create(StateTransition.class, StateTransition.DEFAULT);
    public static final Queue<PlaybackProgressEvent> PLAYBACK_PROGRESS = create(PlaybackProgressEvent.class);
    public static final Queue<PlayableChangedEvent> PLAYABLE_CHANGED = create(PlayableChangedEvent.class);
    public static final Queue<UIEvent> UI = create(UIEvent.class);
    public static final Queue<ActivityLifeCycleEvent> ACTIVITY_LIFE_CYCLE = create(ActivityLifeCycleEvent.class);
    public static final Queue<PlayerLifeCycleEvent> PLAYER_LIFE_CYCLE = create(PlayerLifeCycleEvent.class);
    public static final Queue<String> SCREEN_ENTERED = create("screen", String.class);
    public static final Queue<CurrentUserChangedEvent> CURRENT_USER_CHANGED = create(CurrentUserChangedEvent.class);
    public static final Queue<OnboardingEvent> ONBOARDING = create(OnboardingEvent.class);
    public static final Queue<SearchEvent> SEARCH = create(SearchEvent.class);
    public static final Queue<PlayControlEvent> PLAY_CONTROL = create(PlayControlEvent.class);
    public static final Queue<PlaybackSessionEvent> PLAYBACK_SESSION = create(PlaybackSessionEvent.class);
    public static final Queue<PlaybackPerformanceEvent> PLAYBACK_PERFORMANCE = create(PlaybackPerformanceEvent.class);
    public static final Queue<PlaybackErrorEvent> PLAYBACK_ERROR = create(PlaybackErrorEvent.class);
    public static final Queue<PlayerUIEvent> PLAYER_UI = create(PlayerUIEvent.class);
    public static final Queue<PlayQueueEvent> PLAY_QUEUE = create(PlayQueueEvent.class);
}
