package com.soundcloud.android.users;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.FollowableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.optional.Optional;

import android.support.annotation.NonNull;

@AutoValue
public abstract class UserItem implements ListItem, FollowableItem {

    public static UserItem create(Urn urn, String name, Optional<String> imageUrlTemplate, Optional<String> country, int followersCount, boolean isFollowedByMe) {
        return new AutoValue_UserItem(urn, imageUrlTemplate, name, country, followersCount, isFollowedByMe);
    }

    public static UserItem from(ApiUser apiUser) {
        return from(apiUser, false);
    }

    @NonNull
    public static AutoValue_UserItem from(ApiUser apiUser, boolean isFollowedByMe) {
        return new AutoValue_UserItem(apiUser.getUrn(), apiUser.getAvatarUrlTemplate(), apiUser.getUsername(), Optional.fromNullable(apiUser.getCountry()), apiUser.getFollowersCount(), isFollowedByMe);
    }

    public static UserItem from(User user) {
        return new AutoValue_UserItem(user.urn(), user.avatarUrl(), user.username(), user.country(), user.followersCount(), user.isFollowing());
    }

    public UserItem copyWithFollowing(boolean isFollowedByMe) {
        return new AutoValue_UserItem(getUrn(), getImageUrlTemplate(), name(), country(), followersCount(), isFollowedByMe);
    }

    @VisibleForTesting
    public UserItem copyWithUrn(Urn urn) {
        return new AutoValue_UserItem(urn, getImageUrlTemplate(), name(), country(), followersCount(), isFollowedByMe());
    }

    @Override
    public UserItem updatedWithFollowing(boolean isFollowedByMe, int followingsCount) {
        return new AutoValue_UserItem(getUrn(), getImageUrlTemplate(), name(), country(), followingsCount, isFollowedByMe);
    }

    public abstract String name();

    public abstract Optional<String> country();

    public abstract int followersCount();

    public abstract boolean isFollowedByMe();
}
