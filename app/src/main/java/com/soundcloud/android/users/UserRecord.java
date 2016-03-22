package com.soundcloud.android.users;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.java.optional.Optional;

public interface UserRecord extends ImageResource {

    String getPermalink();

    String getUsername();

    String getCountry();

    int getFollowersCount();

    Optional<String> getDescription();

    Optional<String> getWebsiteUrl();

    Optional<String> getWebsiteName();

    Optional<String> getDiscogsName();

    Optional<String> getMyspaceName();
}
