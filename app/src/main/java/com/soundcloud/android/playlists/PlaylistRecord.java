package com.soundcloud.android.playlists;

import com.soundcloud.android.api.model.Sharing;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.users.UserRecord;
import com.soundcloud.java.functions.Function;

import java.util.Date;

public interface PlaylistRecord extends ImageResource {

    Function<PlaylistRecord, UserRecord> TO_USER_RECORD = input -> input.getUser();

    String getTitle();

    long getDuration();

    Date getCreatedAt();

    Sharing getSharing();

    int getTrackCount();

    UserRecord getUser();

    String getGenre();

    Iterable<String> getTags();

    String getPermalinkUrl();

    int getLikesCount();

    int getRepostsCount();

    boolean isAlbum();

    String getSetType();

    String getReleaseDate();
}
