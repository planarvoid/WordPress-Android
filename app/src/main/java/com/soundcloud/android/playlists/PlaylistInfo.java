package com.soundcloud.android.playlists;

import com.google.common.base.Objects;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

class PlaylistInfo {

    @NotNull private final PropertySet sourceSet;
    @NotNull private final List<PropertySet> tracks;

    PlaylistInfo(@NotNull PropertySet sourceSet, @NotNull List<PropertySet> tracks) {
        this.sourceSet = sourceSet;
        this.tracks = tracks;
    }

    public boolean isLocalPlaylist(){
        return sourceSet.contains(PlaylistProperty.URN) && sourceSet.get(PlaylistProperty.URN).getNumericId() < 0;
    }

    public boolean isMissingMetaData() {
        return sourceSet.size() == 0;
    }

    public boolean needsTracks() {
        return getTracks().isEmpty() && getTrackCount() > 0;
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

    public boolean isOfflineAvailable() {
        return sourceSet.get(PlaylistProperty.IS_MARKED_FOR_OFFLINE);
    }

    @Deprecated // we should avoid this, but apparently we need it to like something currently
    public PropertySet getSourceSet() {
        return sourceSet;
    }

    public void update(PropertySet source) {
        this.sourceSet.update(source);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlaylistInfo)) {
            return false;
        }

        PlaylistInfo that = (PlaylistInfo) o;
        return sourceSet.equals(that.sourceSet) && tracks.equals(that.tracks);
    }

    @Override
    public final int hashCode() {
        return Objects.hashCode(sourceSet, tracks);
    }
}
