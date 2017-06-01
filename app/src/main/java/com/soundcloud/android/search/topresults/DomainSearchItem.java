package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.users.UserItem;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class DomainSearchItem {
    public abstract Optional<UserItem> userItem();

    public abstract Optional<TrackItem> trackItem();

    public abstract Optional<PlaylistItem> playlistItem();

    public static DomainSearchItem track(TrackItem trackItem) {
        return new AutoValue_DomainSearchItem(Optional.absent(), Optional.of(trackItem), Optional.absent());
    }

    public static DomainSearchItem user(UserItem userItem) {
        return new AutoValue_DomainSearchItem(Optional.of(userItem), Optional.absent(), Optional.absent());
    }

    public static DomainSearchItem playlist(PlaylistItem playlistItem) {
        return new AutoValue_DomainSearchItem(Optional.absent(), Optional.absent(), Optional.of(playlistItem));
    }
}
