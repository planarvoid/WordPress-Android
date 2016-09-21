package com.soundcloud.android.playlists;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineProperty;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaylistWithTracks implements ImageResource {

    @NotNull private final PropertySet sourceSet;
    @NotNull private final List<TrackItem> tracks;

    public PlaylistWithTracks(@NotNull PropertySet sourceSet, @NotNull List<TrackItem> tracks) {
        this.sourceSet = sourceSet;
        this.tracks = tracks;
    }

    public PlaylistItem getPlaylistItem() {
        return PlaylistItem.from(sourceSet);
    }

    public boolean isLocalPlaylist() {
        return sourceSet.contains(PlaylistProperty.URN) && sourceSet.get(PlaylistProperty.URN).getNumericId() < 0;
    }

    public boolean isMissingMetaData() {
        return sourceSet.size() == 0;
    }

    public boolean needsTracks() {
        return getTracks().isEmpty();
    }

    @Override
    public Urn getUrn() {
        return sourceSet.get(PlaylistProperty.URN);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return sourceSet.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE, Optional.<String>absent());
    }

    public Urn getCreatorUrn() {
        return sourceSet.get(PlaylistProperty.CREATOR_URN);
    }

    public String getCreatorName() {
        // syncing through public api requires us to fetch usernames lazily. this will be fixed by moving to api-mobile
        return sourceSet.getOrElse(PlaylistProperty.CREATOR_NAME, Strings.EMPTY);
    }

    public String getTitle() {
        return sourceSet.get(PlaylistProperty.TITLE);
    }

    public int getLikesCount() {
        return sourceSet.get(PlaylistProperty.LIKES_COUNT);
    }

    public boolean isLikedByUser() {
        return sourceSet.get(PlaylistProperty.IS_USER_LIKE);
    }

    public int getRepostsCount() {
        return sourceSet.get(PlaylistProperty.REPOSTS_COUNT);
    }

    public boolean isRepostedByUser() {
        return sourceSet.getOrElse(PlaylistProperty.IS_USER_REPOST, false);
    }

    public boolean isPostedByUser() {
        return sourceSet.get(PlaylistProperty.IS_POSTED);
    }

    public boolean isOwnedBy(Urn userUrn) {
        return userUrn.equals(getCreatorUrn());
    }

    @NotNull
    public List<TrackItem> getTracks() {
        return tracks;
    }

    public List<Urn> getTracksUrn() {
        return Lists.transform(tracks, PlayableItem.TO_URN);
    }

    public String getDuration() {
        final long duration = tracks.isEmpty() ?
                              sourceSet.get(PlaylistProperty.PLAYLIST_DURATION) :
                              getCombinedTrackDurations();
        return ScTextUtils.formatTimestamp(duration, TimeUnit.MILLISECONDS);
    }

    public OfflineState getDownloadState() {
        return sourceSet.getOrElse(OfflineProperty.OFFLINE_STATE, OfflineState.NOT_OFFLINE);
    }

    private long getCombinedTrackDurations() {
        long duration = 0;
        for (TrackItem track : tracks) {
            duration += track.getDuration();
        }
        return duration;
    }

    public int getTrackCount() {
        return tracks.isEmpty()
               ? sourceSet.get(PlaylistProperty.TRACK_COUNT)
               : tracks.size();
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
        return sourceSet.get(OfflineProperty.IS_MARKED_FOR_OFFLINE);
    }

    public void update(PropertySet source) {
        this.sourceSet.update(source);
    }

    @Deprecated // we should avoid this, but apparently we need it to like something currently
    public PropertySet getSourceSet() {
        return sourceSet;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PlaylistWithTracks)) {
            return false;
        }

        PlaylistWithTracks that = (PlaylistWithTracks) o;
        return sourceSet.equals(that.sourceSet) && tracks.equals(that.tracks);
    }

    @Override
    public final int hashCode() {
        return MoreObjects.hashCode(sourceSet, tracks);
    }
}