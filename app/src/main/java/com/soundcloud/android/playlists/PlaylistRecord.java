package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.users.UserRecord;

import java.util.Date;

public interface PlaylistRecord extends ImageResource {

    String getTitle();

    long getDuration();

    Date getCreatedAt();

    Sharing getSharing();

    int getTrackCount();

    UserRecord getUser();

    Iterable<String> getTags();

    String getPermalinkUrl();

    int getLikesCount();

    int getRepostsCount();
}
