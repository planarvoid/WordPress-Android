package com.soundcloud.android.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

@AutoValue
public abstract class ApiSocialMediaLink {

    @JsonCreator
    public static ApiSocialMediaLink create(@JsonProperty("title") Optional<String> title,
                                            @JsonProperty("network") String network,
                                            @JsonProperty("url") String url) {
        return new AutoValue_ApiSocialMediaLink(
                title.filter(Strings::isNotBlank),
                network,
                url);
    }

    public static ApiSocialMediaLink create(String network, String url) {
        return new AutoValue_ApiSocialMediaLink(Optional.absent(), network, url);
    }

    public abstract Optional<String> title();

    public abstract String network();

    public abstract String url();

}
