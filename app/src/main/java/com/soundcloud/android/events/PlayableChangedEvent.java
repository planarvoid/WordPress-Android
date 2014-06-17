package com.soundcloud.android.events;

import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

public final class PlayableChangedEvent {

    private final Playable playable;
    private final PropertySet changeSet;

    public static PlayableChangedEvent create(Playable playable, PropertySet changeSet) {
        return new PlayableChangedEvent(playable, changeSet);
    }

    public static PlayableChangedEvent forLike(Playable playable, boolean liked) {
        return new PlayableChangedEvent(playable, PropertySet.from(
                PlayableProperty.IS_LIKED.bind(liked),
                PlayableProperty.LIKES_COUNT.bind(playable.likes_count)));
    }

    public static PlayableChangedEvent forRepost(Playable playable, boolean reposted) {
        return new PlayableChangedEvent(playable, PropertySet.from(
                PlayableProperty.IS_REPOSTED.bind(reposted),
                PlayableProperty.REPOSTS_COUNT.bind(playable.reposts_count)));
    }

    private PlayableChangedEvent(Playable playable, PropertySet changeSet) {
        this.playable = playable;
        this.changeSet = changeSet;
    }

    @Deprecated // don't rely on Public API models going forward, use PropertySets
    public Playable getPlayable() {
        return playable;
    }

    public Urn getUrn() {
        return playable.getUrn();
    }

    public PropertySet getChangeSet() {
        return changeSet;
    }
}
