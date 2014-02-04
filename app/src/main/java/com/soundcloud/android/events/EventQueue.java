package com.soundcloud.android.events;

import static com.soundcloud.android.events.EventBus2.QueueDescriptor;

public interface EventQueue {
    final QueueDescriptor<PlaybackEvent> PLAYBACK =
            QueueDescriptor.create("playback_queue", PlaybackEvent.class);

    final QueueDescriptor<PlayableChangedEvent> PLAYABLE_CHANGED =
            QueueDescriptor.create("playable_changed", PlayableChangedEvent.class);

    final QueueDescriptor<UIEvent> UI = QueueDescriptor.create("ui", UIEvent.class);

    final QueueDescriptor<ActivityLifeCycleEvent> ACTIVITY_LIFE_CYCLE =
            QueueDescriptor.create("activity_life_cycle", ActivityLifeCycleEvent.class);
}
