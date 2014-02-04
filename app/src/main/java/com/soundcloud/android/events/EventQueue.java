package com.soundcloud.android.events;

import static com.soundcloud.android.events.EventBus2.QueueDescriptor;

public interface EventQueue {
    QueueDescriptor<PlaybackEvent> PLAYBACK = QueueDescriptor.create("playback_queue");
    QueueDescriptor<PlayableChangedEvent> PLAYABLE_CHANGED = QueueDescriptor.create("playable_changed");
}
