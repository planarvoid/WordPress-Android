package com.soundcloud.android.users;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUserProfileInfo;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class UserProfileInfo {
    public static UserProfileInfo fromApiUserProfileInfo(ApiUserProfileInfo apiUserProfileInfo) {
        return create(
                apiUserProfileInfo.getSocialMediaLinks().transform(SocialMediaLinkItem::from),
                apiUserProfileInfo.getDescription(),
                User.fromApiUser(apiUserProfileInfo.getUser())
        );
    }

    public static UserProfileInfo create(ModelCollection<SocialMediaLinkItem> socialMediaLinks,
                                         Optional<String> description,
                                         User user) {
        return new AutoValue_UserProfileInfo(socialMediaLinks, description, user);
    }

    public abstract ModelCollection<SocialMediaLinkItem> getSocialMediaLinks();

    public abstract Optional<String> getDescription();

    public abstract User getUser();
}
