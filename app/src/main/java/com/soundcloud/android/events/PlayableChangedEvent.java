package com.soundcloud.android.events;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;

public final class PlayableChangedEvent {

    private final Urn soundUrn;
    private final PropertySet changeSet;

    public static PlayableChangedEvent create(Urn soundUrn, PropertySet changeSet) {
        return new PlayableChangedEvent(soundUrn, changeSet);
    }

    public static PlayableChangedEvent forLike(Urn soundUrn, boolean liked, int likesCount) {
        return new PlayableChangedEvent(soundUrn, PropertySet.from(
                PlayableProperty.IS_LIKED.bind(liked),
                PlayableProperty.LIKES_COUNT.bind(likesCount)));
    }

    public static PlayableChangedEvent forRepost(Urn soundUrn, boolean reposted, int repostCount) {
        return new PlayableChangedEvent(soundUrn, PropertySet.from(
                PlayableProperty.IS_REPOSTED.bind(reposted),
                PlayableProperty.REPOSTS_COUNT.bind(repostCount)));
    }

    private PlayableChangedEvent(Urn soundUrn, PropertySet changeSet) {
        this.soundUrn = soundUrn;
        this.changeSet = changeSet;
    }

    public Urn getUrn() {
        return soundUrn;
    }

    public PropertySet getChangeSet() {
        return changeSet;
    }
}
