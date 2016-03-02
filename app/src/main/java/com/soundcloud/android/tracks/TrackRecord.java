package com.soundcloud.android.tracks;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public interface TrackRecord  {
    UserRecord getUser();

    Urn getUrn();

    String getTitle();

    long getSnippetDuration();

    long getFullDuration();

    String getWaveformUrl();

    String getStreamUrl();

    String getPermalinkUrl();

    Date getCreatedAt();

    String getGenre();

    Sharing getSharing();

    boolean isCommentable();

    boolean isMonetizable();

    boolean isBlocked();

    boolean isSnipped();

    String getPolicy();

    Optional<String> getMonetizationModel();

    Optional<Boolean> isSubMidTier();

    Optional<Boolean> isSubHighTier();

    boolean isSyncable();

    int getPlaybackCount();

    int getCommentsCount();

    int getLikesCount();

    int getRepostsCount();

    Optional<String> getDescription();
}
