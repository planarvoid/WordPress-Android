package com.soundcloud.android.users;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import java.util.Date;

public interface UserRecord extends ImageResource {

    String getPermalink();

    String getUsername();

    Optional<String> getFirstName();

    Optional<String> getLastName();

    Optional<Date> getCreatedAt();

    String getCountry();

    String getCity();

    int getFollowersCount();

    int getFollowingsCount();

    Optional<String> getDescription();

    Optional<String> getWebsiteUrl();

    Optional<String> getWebsiteName();

    Optional<String> getDiscogsName();

    Optional<String> getMyspaceName();

    Optional<Urn> getArtistStationUrn();

    Optional<String> getVisualUrlTemplate();

    boolean isPro();
}
