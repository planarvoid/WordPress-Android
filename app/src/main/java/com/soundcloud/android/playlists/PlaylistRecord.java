package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.Sharing;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserRecord;

import java.util.Date;

public interface PlaylistRecord {
    Urn getUrn();

    String getTitle();

    int getDuration();

    Date getCreatedAt();

    Sharing getSharing();

    int getTrackCount();

    UserRecord getUser();

    Iterable<String> getTags();

    String getPermalinkUrl();

    int getLikesCount();

    int getRepostsCount();
}
