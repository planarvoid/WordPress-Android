package com.soundcloud.android.tracks;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;

import java.util.Date;

public interface TrackRecord {
    UserRecord getUser();

    Urn getUrn();

    String getTitle();

    int getDuration();

    String getWaveformUrl();

    String getStreamUrl();

    String getPermalinkUrl();

    Date getCreatedAt();

    String getGenre();

    Sharing getSharing();

    boolean isCommentable();

    boolean isMonetizable();

    String getPolicy();

    boolean isSyncable();

    int getPlaybackCount();

    int getCommentsCount();

    int getLikesCount();

    int getRepostsCount();
}
