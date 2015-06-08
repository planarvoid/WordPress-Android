package com.soundcloud.android.users;

import com.google.common.base.Optional;
import com.soundcloud.android.model.Urn;

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
