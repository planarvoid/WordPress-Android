package com.soundcloud.android.users;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public interface UserRecord extends ImageResource {

    String getPermalink();

    String getUsername();

    String getCountry();

    String getCity();

    int getFollowersCount();

    Optional<String> getDescription();

    Optional<String> getWebsiteUrl();

    Optional<String> getWebsiteName();

    Optional<String> getDiscogsName();

    Optional<String> getMyspaceName();

    Optional<Urn> getArtistStationUrn();

    Optional<String> getVisualUrlTemplate();
}
