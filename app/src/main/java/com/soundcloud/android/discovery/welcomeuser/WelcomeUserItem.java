package com.soundcloud.android.discovery.welcomeuser;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.discovery.DiscoveryItem;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.User;
import com.soundcloud.android.utils.DateUtils;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class WelcomeUserItem extends DiscoveryItem implements ImageResource {

    public abstract Urn userUrn();
    public abstract String userName();
    public abstract String avatarUrl();
    public abstract TimeOfDay timeOfDay();
    public abstract boolean isNewSignup();

    public static DiscoveryItem create(User user) {
        String name = user.firstName().isPresent() ? user.firstName().get() : user.username();
        TimeOfDay timeOfDay = TimeOfDay.getCurrent();
        boolean newSignup = DateUtils.isInLastHours(user.signupDate(), 2);
        return new AutoValue_WelcomeUserItem(Kind.WelcomeUserItem, user.urn(), name, user.avatarUrl().get(), timeOfDay, newSignup);
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
