package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class ApiSocialMediaLink {

    @JsonCreator
    public static ApiSocialMediaLink create(@JsonProperty("title") String title,
                                            @JsonProperty("network") String network,
                                            @JsonProperty("url") String url) {
        return new AutoValue_ApiSocialMediaLink(Optional.fromNullable(title), network, url);
    }

    public static ApiSocialMediaLink create(String network, String url) {
        return new AutoValue_ApiSocialMediaLink(Optional.absent(), network, url);
    }

    public abstract Optional<String> title();

    public abstract String network();

    public abstract String url();

}
