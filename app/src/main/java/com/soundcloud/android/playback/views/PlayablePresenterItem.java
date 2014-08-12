package com.soundcloud.android.playback.views;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import android.content.res.Resources;

public class PlayablePresenterItem {

    private final PropertySet propertySet;

    public PlayablePresenterItem(PropertySet propertySet) {
        this.propertySet = propertySet;
    }

    public long getLikesCount() {
        return propertySet.get(PlayableProperty.LIKES_COUNT);
    }

    public long getRepostsCount() {
        return propertySet.get(PlayableProperty.REPOSTS_COUNT);
    }

    public boolean isRepostedByCurrentUser() {
        return propertySet.get(PlayableProperty.IS_REPOSTED);
    }

    public boolean isLikedByCurrentUser() {
        return propertySet.get(PlayableProperty.IS_LIKED);
    }

    public int getPlaysCount() {
        return propertySet.getOrElse(TrackProperty.PLAY_COUNT, 0);
    }

    public long getCommentsCount() {
        return propertySet.getOrElse(TrackProperty.COMMENTS_COUNT, 0);
    }

    public boolean isPrivate() {
        return propertySet.get(PlayableProperty.IS_PRIVATE);
    }

    public String getTimeSinceCreated(Resources resources) {
        return ScTextUtils.formatTimeElapsed(resources, propertySet.get(PlayableProperty.CREATED_AT).getTime());
    }

    public Urn getUrn() {
        if (propertySet.contains(TrackProperty.URN)) {
            return propertySet.get(TrackProperty.URN);
        }
        return propertySet.get(PlayableProperty.URN);
    }

    public String getCreatorName() {
        return propertySet.get(PlayableProperty.CREATOR_NAME);
    }

    public String getTitle() {
        return propertySet.get(PlayableProperty.TITLE);
    }
}
