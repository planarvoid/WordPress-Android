package com.soundcloud.android.discovery.systemplaylist;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Date;
import java.util.List;

@AutoValue
public abstract class SystemPlaylistEntity {

    public abstract Urn urn();

    public abstract Optional<Urn> queryUrn();

    public abstract Optional<String> title();

    public abstract Optional<String> description();

    public abstract Optional<String> artworkUrlTemplate();

    public abstract Optional<String> trackingFeatureName();

    public abstract List<Urn> trackUrns();

    public abstract Optional<Date> lastUpdated();

    public static SystemPlaylistEntity create(Urn urn,
                                              Optional<Urn> queryUrn,
                                              Optional<String> title,
                                              Optional<String> description,
                                              List<Urn> trackUrns,
                                              Optional<Date> lastUpdated,
                                              Optional<String> artworkUrlTemplate,
                                              Optional<String> trackingFeatureName) {
        return new AutoValue_SystemPlaylistEntity(urn, queryUrn, title, description, artworkUrlTemplate, trackingFeatureName, trackUrns, lastUpdated);
    }
}
