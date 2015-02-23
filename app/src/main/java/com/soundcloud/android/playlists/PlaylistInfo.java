package com.soundcloud.android.playlists;

import com.soundcloud.android.api.legacy.model.PublicApiPlaylist;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.CollectionUtils;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

import java.util.List;
import java.util.concurrent.TimeUnit;

class PlaylistInfo {

    private final PropertySet sourceSet;
    private final List<PropertySet> tracks;

    PlaylistInfo(PublicApiPlaylist publicApiPlaylist) {
        sourceSet = publicApiPlaylist.toPropertySet();
        tracks = CollectionUtils.toPropertySets(publicApiPlaylist.getTracks());
    }

    PlaylistInfo(PropertySet sourceSet, List<PropertySet> tracks) {
        this.sourceSet = sourceSet;
        this.tracks = tracks;
    }

    public Urn getUrn() {
        return sourceSet.get(PlaylistProperty.URN);
    }

    public Urn getCreatorUrn() {
        return sourceSet.get(PlaylistProperty.CREATOR_URN);
    }

    public String getCreatorName() {
        // syncing through public api requires us to fetch usernames lazily. this will be fixed by moving to api-mobile
        return sourceSet.getOrElse(PlaylistProperty.CREATOR_NAME, ScTextUtils.EMPTY_STRING);
    }

    public String getTitle() {
        return sourceSet.get(PlaylistProperty.TITLE);
    }

    public int getLikesCount() {
        return sourceSet.get(PlaylistProperty.LIKES_COUNT);
    }

    public boolean isLikedByUser() {
        return sourceSet.get(PlaylistProperty.IS_LIKED);
    }

    public int getRepostsCount() {
        return sourceSet.get(PlaylistProperty.REPOSTS_COUNT);
    }

    public boolean isRepostedByUser() {
        return sourceSet.get(PlaylistProperty.IS_REPOSTED);
    }

    public List<PropertySet> getTracks() {
        return tracks;
    }

    public String getDuration() {
        return ScTextUtils.formatTimestamp(sourceSet.get(PlaylistProperty.DURATION), TimeUnit.MILLISECONDS);
    }

    public int getTrackCount() {
        return sourceSet.get(PlaylistProperty.TRACK_COUNT);
    }

    public boolean isPrivate() {
        return sourceSet.get(PlaylistProperty.IS_PRIVATE);
    }

    public boolean isPublic() {
        return !isPrivate();
    }

    public String getPermalinkUrl() {
        return sourceSet.get(PlaylistProperty.PERMALINK_URL);
    }

    @Deprecated // we should avoid this, but apparently we need it to like something currently
    public PropertySet getSourceSet() {
        return sourceSet;
    }

    public void update(PropertySet source) {
        this.sourceSet.update(source);
    }
}
