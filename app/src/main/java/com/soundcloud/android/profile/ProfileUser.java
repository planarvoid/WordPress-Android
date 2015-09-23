package com.soundcloud.android.profile;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.java.collections.PropertySet;

class ProfileUser {

    private final PropertySet source;

    public ProfileUser(PropertySet source) {
        this.source = source;
    }

    public Urn getUrn() {
        return source.get(UserProperty.URN);
    }

    public String getName() {
        return source.get(UserProperty.USERNAME);
    }

    public long getFollowerCount() {
        return source.get(UserProperty.FOLLOWERS_COUNT);
    }

    public boolean isFollowed() {
        return source.get(UserProperty.IS_FOLLOWED_BY_ME);
    }

    public String getDescription() {
        return source.getOrElseNull(UserProperty.DESCRIPTION);
    }

    public String getDiscogsName() {
        return source.getOrElseNull(UserProperty.DISCOGS_NAME);
    }

    public String getWebsiteUrl() {
        return source.getOrElseNull(UserProperty.WEBSITE_URL);
    }

    public String getWebsiteName() {
        return source.getOrElseNull(UserProperty.WEBSITE_NAME);
    }

    public String getMyspaceName() {
        return source.getOrElseNull(UserProperty.MYSPACE_NAME);
    }

    public boolean hasDescription() {
        return source.contains(UserProperty.DESCRIPTION);
    }

    public boolean hasDetails() {
        return ScTextUtils.isNotBlank(source.getOrElseNull(UserProperty.DESCRIPTION))
                || ScTextUtils.isNotBlank(source.getOrElseNull(UserProperty.DISCOGS_NAME))
                || ScTextUtils.isNotBlank(source.getOrElseNull(UserProperty.WEBSITE_URL))
                || ScTextUtils.isNotBlank(source.getOrElseNull(UserProperty.MYSPACE_NAME));
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ProfileUser && source.equals(((ProfileUser) o).source);
    }

    @Override
    public int hashCode() {
        return source.hashCode();
    }
}
