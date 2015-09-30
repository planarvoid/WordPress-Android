package com.soundcloud.android.events;

import static com.appboy.ui.support.StringUtils.EMPTY_STRING;

import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.java.collections.PropertySet;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class PlayableMetadata {
    public static final String KEY_CREATOR_NAME = "creator_display_name";
    public static final String KEY_CREATOR_URN = "creator_urn";
    public static final String KEY_PLAYABLE_TITLE = "playable_title";
    public static final String KEY_PLAYABLE_URN = "playable_urn";
    public static final String KEY_PLAYABLE_TYPE = "playable_type";

    private static final String TYPE_TRACK = "track";
    private static final String TYPE_PLAYLIST = "playlist";
    private static final String TYPE_STATION = "station";
    private static final String TYPE_OTHER = "other";

    private final String creatorName;
    private final Urn creatorUrn;
    private final String playableTitle;
    private final Urn playableUrn;

    PlayableMetadata(String creatorName, Urn creatorUrn, String playableTitle, Urn playableUrn) {
        this.creatorName = creatorName;
        this.creatorUrn = creatorUrn;
        this.playableTitle = playableTitle;
        this.playableUrn = playableUrn;
    }

    static PlayableMetadata fromPlayableProperties(@Nullable PropertySet properties) {
        if (properties == null) {
            return empty();
        }

        return new PlayableMetadata(
                properties.getOrElse(PlayableProperty.CREATOR_NAME, EMPTY_STRING),
                properties.getOrElse(PlayableProperty.CREATOR_URN, Urn.NOT_SET),
                properties.getOrElse(PlayableProperty.TITLE, EMPTY_STRING),
                properties.getOrElse(PlayableProperty.URN, Urn.NOT_SET));
    }

    static PlayableMetadata empty() {
        return new PlayableMetadata(EMPTY_STRING, Urn.NOT_SET, EMPTY_STRING, Urn.NOT_SET);
    }

    static PlayableMetadata fromPlayableItem(@Nullable PlayableItem item) {
        if (item == null) {
            return empty();
        }

        return new PlayableMetadata(
                item.getCreatorName(),
                item.getCreatorUrn(),
                item.getTitle(),
                item.getEntityUrn());
    }

    void addToTrackingEvent(@NonNull TrackingEvent event) {
        event.put(KEY_CREATOR_NAME, creatorName)
                .put(KEY_CREATOR_URN, creatorUrn.toString())
                .put(KEY_PLAYABLE_TITLE, playableTitle)
                .put(KEY_PLAYABLE_URN, playableUrn.toString())
                .put(KEY_PLAYABLE_TYPE, getPlayableType());
    }

    private String getPlayableType() {
        if (playableUrn.isTrack()) {
            return TYPE_TRACK;
        } else if (playableUrn.isPlaylist()) {
            return TYPE_PLAYLIST;
        } else if (playableUrn.isStation()) {
            return TYPE_STATION;
        } else {
            return TYPE_OTHER;
        }
    }

}
