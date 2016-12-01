package com.soundcloud.android.profile;

import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

public class ProfileUser implements ImageResource {

    private final PropertySet source;

    public ProfileUser(PropertySet source) {
        this.source = source;
    }

    @Override
    public Urn getUrn() {
        return source.get(UserProperty.URN);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE, Optional.<String>absent());
    }

    public Optional<String> getVisualUrl() {
        return source.getOrElse(UserProperty.VISUAL_URL, Optional.<String>absent());
    }

    public Optional<Urn> getArtistStationUrn() {
        return source.getOrElse(UserProperty.ARTIST_STATION, Optional.<Urn>absent());
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
        return Strings.isNotBlank(source.getOrElseNull(UserProperty.DESCRIPTION))
                || Strings.isNotBlank(source.getOrElseNull(UserProperty.DISCOGS_NAME))
                || Strings.isNotBlank(source.getOrElseNull(UserProperty.WEBSITE_URL))
                || Strings.isNotBlank(source.getOrElseNull(UserProperty.MYSPACE_NAME));
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
