package com.soundcloud.android.events;

import static com.soundcloud.android.events.EventBus.QueueDescriptor;
import static com.soundcloud.android.events.EventBus.QueueDescriptor.create;

public interface EventQueue {
    final QueueDescriptor<PlaybackEvent> PLAYBACK = create(PlaybackEvent.class);
    final QueueDescriptor<PlayableChangedEvent> PLAYABLE_CHANGED = create(PlayableChangedEvent.class);
    final QueueDescriptor<UIEvent> UI = create(UIEvent.class);
    final QueueDescriptor<ActivityLifeCycleEvent> ACTIVITY_LIFE_CYCLE = create(ActivityLifeCycleEvent.class);
    final QueueDescriptor<PlayerLifeCycleEvent> PLAYER_LIFE_CYCLE = create(PlayerLifeCycleEvent.class);
    final QueueDescriptor<String> SCREEN_ENTERED = create("screen", String.class);
    final QueueDescriptor<CurrentUserChangedEvent> CURRENT_USER_CHANGED = create(CurrentUserChangedEvent.class);
    final QueueDescriptor<OnboardingEvent> ONBOARDING = create(OnboardingEvent.class);
    final QueueDescriptor<SearchEvent> SEARCH = create(SearchEvent.class);
    final QueueDescriptor<PlayControlEvent> PLAY_CONTROL = create(PlayControlEvent.class);

}
