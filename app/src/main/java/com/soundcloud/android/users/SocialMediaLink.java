package com.soundcloud.android.users;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiSocialMediaLink;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class SocialMediaLink {

    public static SocialMediaLink from(ApiSocialMediaLink apiSocialMediaLink) {
        return create(apiSocialMediaLink.title(), apiSocialMediaLink.network(), apiSocialMediaLink.url());
    }

    public static SocialMediaLink create(Optional<String> title, String network, String url) {
        return new AutoValue_SocialMediaLink(title, network, url);
    }

    public abstract Optional<String> title();

    public abstract String network();

    public abstract String url();
}
