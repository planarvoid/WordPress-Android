package com.soundcloud.android.tracks;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public interface TrackRecord extends ImageResource {
    Function<TrackRecord, UserRecord> TO_USER_RECORD = input -> input.getUser();

    UserRecord getUser();

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
