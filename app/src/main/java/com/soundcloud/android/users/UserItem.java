package com.soundcloud.android.users;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.FollowableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.UpdatableUserItem;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class UserItem implements ListItem, FollowableItem, UpdatableUserItem {

    public static UserItem from(User user) {
        return builder().user(user).isFollowedByMe(user.isFollowing()).build();
    }

    public abstract User user();

    public abstract boolean isFollowedByMe();

    public Urn getUrn(){
        return user().urn();
    }

    public Optional<String> getImageUrlTemplate(){
        return user().avatarUrl();
    }

    public String name(){
        return user().username();
    }

    public Optional<String> country(){
        return user().country();
    }

    public int followersCount(){
        return user().followersCount();
    }

    public abstract UserItem.Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_UserItem.Builder();
    }

    public UserItem copyWithFollowing(boolean isFollowedByMe) {
        return toBuilder().isFollowedByMe(isFollowedByMe).build();
    }

    @Override
    public UserItem updatedWithFollowing(boolean isFollowedByMe, int followingsCount) {
        return toBuilder().user(user().toBuilder().followingsCount(followingsCount).build()).isFollowedByMe(isFollowedByMe).build();
    }

    @Override
    public UpdatableUserItem updateWithUser(User user) {
        return toBuilder().user(user).build();
    }


    @AutoValue.Builder
    public abstract static class Builder {

        public abstract Builder user(User user);

        public abstract Builder isFollowedByMe(boolean isFollowedByMe);

        public abstract UserItem build();
    }
}
