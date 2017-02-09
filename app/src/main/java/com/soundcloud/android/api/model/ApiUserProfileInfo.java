package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ApiUserProfileInfo {

    @JsonCreator
    public static ApiUserProfileInfo create(
            @JsonProperty("social_media_links") ModelCollection<ApiSocialMediaLink> socialMediaLinks,
            @JsonProperty("description") Optional<String> description,
            @JsonProperty("user") ApiUser user) {
        return new AutoValue_ApiUserProfileInfo(socialMediaLinks, description, user);
    }

    public abstract ModelCollection<ApiSocialMediaLink> getSocialMediaLinks();

    public abstract Optional<String> getDescription();

    public abstract ApiUser getUser();

}
