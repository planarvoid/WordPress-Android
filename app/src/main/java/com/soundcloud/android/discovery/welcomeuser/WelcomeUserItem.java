package com.soundcloud.android.discovery.welcomeuser;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.profile.ProfileUser;
import com.soundcloud.android.utils.DateUtils;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class WelcomeUserItem extends DiscoveryItem implements ImageResource {

    public abstract Urn userUrn();
    public abstract String userName();
    public abstract String avatarUrl();
    public abstract TimeOfDay timeOfDay();
    public abstract boolean isNewSignup();

    public static DiscoveryItem create(ProfileUser user) {
        String name = user.getFirstName().isPresent() ? user.getFirstName().get() : user.getName();
        TimeOfDay timeOfDay = TimeOfDay.getCurrent();
        boolean newSignup = DateUtils.isInLastHours(user.getSignupDate(), 2);
        return new AutoValue_WelcomeUserItem(Kind.WelcomeUserItem, user.getUrn(), name, user.getImageUrlTemplate().get(), timeOfDay, newSignup);
    }

    @Override
    public Urn getUrn() {
        return userUrn();
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.of(avatarUrl());
    }
}
