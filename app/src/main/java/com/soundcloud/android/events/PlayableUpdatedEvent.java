package com.soundcloud.android.events;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.propeller.PropertySet;
import rx.functions.Func1;

public final class PlayableUpdatedEvent {

    private final Urn soundUrn;
    private final PropertySet changeSet;
    private final int reason;

    private static final int REASON_UPDATED = 0;
    private static final int REASON_REPOST = 1;
    private static final int REASON_LIKE = 2;

    public static final Func1<PlayableUpdatedEvent, Boolean> IS_TRACK_FILTER = new Func1<PlayableUpdatedEvent, Boolean>() {
        @Override
        public Boolean call(PlayableUpdatedEvent event) {
            return event.getUrn().isTrack();
        }
    };

    public static PlayableUpdatedEvent forUpdate(Urn soundUrn, PropertySet changeSet) {
        return new PlayableUpdatedEvent(soundUrn, changeSet, REASON_UPDATED);
    }

    public static PlayableUpdatedEvent forLike(Urn soundUrn, boolean liked, int likesCount) {
        return new PlayableUpdatedEvent(soundUrn, PropertySet.from(
                PlayableProperty.IS_LIKED.bind(liked),
                PlayableProperty.LIKES_COUNT.bind(likesCount)), REASON_LIKE);
    }

    public static PlayableUpdatedEvent forRepost(Urn soundUrn, boolean reposted, int repostCount) {
        return new PlayableUpdatedEvent(soundUrn, PropertySet.from(
                PlayableProperty.IS_REPOSTED.bind(reposted),
                PlayableProperty.REPOSTS_COUNT.bind(repostCount)), REASON_REPOST);
    }

    private PlayableUpdatedEvent(Urn soundUrn, PropertySet changeSet, int reason) {
        this.soundUrn = soundUrn;
        this.changeSet = changeSet;
        this.reason = reason;
    }

    public Urn getUrn() {
        return soundUrn;
    }

    public PropertySet getChangeSet() {
        return changeSet;
    }

    public boolean wasLiked(){
        return reason == REASON_LIKE;
    }

    public boolean wasReposted(){
        return reason == REASON_REPOST;
    }

    public boolean wasUpdated(){
        return reason == REASON_UPDATED;
    }
}
