package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

public interface UserRecord {

    Urn getUrn();

    String getUsername();

    String getCountry();

    int getFollowersCount();

    @Deprecated String getAvatarUrl();

    Optional<String> getDescription();

    Optional<String> getWebsiteUrl();

    Optional<String> getWebsiteName();

    Optional<String> getDiscogsName();

    Optional<String> getMyspaceName();
}
