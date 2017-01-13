package com.soundcloud.android.users;

import auto.parcel.AutoParcel;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.FollowableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.search.SearchableItem;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;

@AutoParcel
public abstract class UserItem implements ListItem, FollowableItem, SearchableItem {

    public static UserItem from(PropertySet source) {
        return create(source.get(UserProperty.URN),
                      source.get(UserProperty.USERNAME),
                      source.get(UserProperty.IMAGE_URL_TEMPLATE),
                      Optional.fromNullable(source.getOrElseNull(UserProperty.COUNTRY)),
                      source.get(UserProperty.FOLLOWERS_COUNT),
                      source.getOrElse(UserProperty.IS_FOLLOWED_BY_ME, false));
    }

    public static UserItem create(Urn urn, String name, Optional<String> imageUrlTemplate, Optional<String> country, int followersCount, boolean isFollowedByMe) {
        return new AutoParcel_UserItem(imageUrlTemplate, urn, name, country, followersCount, isFollowedByMe);
    }

    public static UserItem from(ApiUser apiUser) {
        return new AutoParcel_UserItem(apiUser.getAvatarUrlTemplate(), apiUser.getUrn(), apiUser.getUsername(), Optional.fromNullable(apiUser.getCountry()), apiUser.getFollowersCount(), false);
    }

    public static UserItem from(User user) {
        return new AutoParcel_UserItem(user.avatarUrl(), user.urn(), user.username(), user.country(), user.followersCount(), user.isFollowing());
    }

    public UserItem copyWithFollowing(boolean isFollowedByMe) {
        return new AutoParcel_UserItem(getImageUrlTemplate(), getUrn(), name(), country(), followersCount(), isFollowedByMe);
    }

    @VisibleForTesting
    public UserItem copyWithUrn(Urn urn) {
        return new AutoParcel_UserItem(getImageUrlTemplate(), urn, name(), country(), followersCount(), isFollowedByMe());
    }

    @Override
    public UserItem updatedWithFollowing(boolean isFollowedByMe, int followingsCount) {
        return new AutoParcel_UserItem(getImageUrlTemplate(), getUrn(), name(), country(), followingsCount, isFollowedByMe);
    }

    public abstract String name();

    public abstract Optional<String> country();

    public abstract int followersCount();

    public abstract boolean isFollowedByMe();
}
