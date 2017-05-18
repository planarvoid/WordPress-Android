package com.soundcloud.android.discovery.systemplaylist;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class SystemPlaylist {

    public abstract Urn urn();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<String> title();

    public abstract Optional<String> description();

    public abstract Optional<String> artworkUrlTemplate();

    public abstract List<Track> tracks();

    public abstract Optional<Date> lastUpdated();

    public static SystemPlaylist create(Urn urn, Optional<Urn> queryUrn, Optional<String> title, Optional<String> description, List<Track> tracks, Optional<Date> lastUpdated, Optional<String> artworkUrlTemplate) {
        return new AutoValue_SystemPlaylist(urn, queryUrn, title, description, artworkUrlTemplate, tracks, lastUpdated);
    }

    public Optional<ImageResource> imageResource() {
        if (artworkUrlTemplate().isPresent()) {
            return Optional.of(SimpleImageResource.create(urn(), artworkUrlTemplate()));
        } else if (tracks().isEmpty()) {
            return Optional.absent();
        } else {
            return Optional.of(SimpleImageResource.create(tracks().get(0)));
        }
    }
}
