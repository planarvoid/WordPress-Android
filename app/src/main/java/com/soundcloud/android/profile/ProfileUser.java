package com.soundcloud.android.profile;

import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import java.util.Date;

public class ProfileUser implements ImageResource {

    private final PropertySet source;

    public ProfileUser(PropertySet source) {
        this.source = source;
    }

    public static ProfileUser from(ApiUser apiUser) {
        final PropertySet bindings = PropertySet.from(
                UserProperty.URN.bind(apiUser.getUrn()),
                UserProperty.USERNAME.bind(apiUser.getUsername()),
                UserProperty.FOLLOWERS_COUNT.bind(apiUser.getFollowersCount()),
                UserProperty.IMAGE_URL_TEMPLATE.bind(apiUser.getAvatarUrlTemplate()),
                UserProperty.SIGNUP_DATE.bind(apiUser.getCreatedAt()),
                UserProperty.FIRST_NAME.bind(apiUser.getFirstName()),
                UserProperty.LAST_NAME.bind(apiUser.getLastName())
        );
        // this should be modeled with an Option type instead:
        // https://github.com/soundcloud/propeller/issues/32
        if (apiUser.getCountry() != null) {
            bindings.put(UserProperty.COUNTRY, apiUser.getCountry());
        }

        if (apiUser.getCity() != null) {
            bindings.put(UserProperty.CITY, apiUser.getCity());
        }
        return new ProfileUser(bindings);
    }

    @Override
    public Urn getUrn() {
        return source.get(UserProperty.URN);
    }

    public void setUrn(Urn urn) {
        source.put(UserProperty.URN, urn);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getOrElse(EntityProperty.IMAGE_URL_TEMPLATE, Optional.absent());
    }

    public Optional<String> getVisualUrl() {
        return source.getOrElse(UserProperty.VISUAL_URL, Optional.absent());
    }

    public Optional<Urn> getArtistStationUrn() {
        return source.getOrElse(UserProperty.ARTIST_STATION, Optional.absent());
    }

    public String getName() {
        return source.get(UserProperty.USERNAME);
    }

    public Optional<String> getFirstName() {
        return source.getOrElse(UserProperty.FIRST_NAME, Optional.absent());
    }

    public Optional<String> getLastName() {
        return source.getOrElse(UserProperty.LAST_NAME, Optional.absent());
    }

    public Optional<Date> getSignupDate() {
        return source.getOrElse(UserProperty.SIGNUP_DATE, Optional.absent());
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
