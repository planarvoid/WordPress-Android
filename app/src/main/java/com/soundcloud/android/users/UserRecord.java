package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;

public interface UserRecord {

    Urn getUrn();

    String getUsername();

    String getCountry();

    int getFollowersCount();

    @Deprecated String getAvatarUrl();
}
