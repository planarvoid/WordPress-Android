package com.soundcloud.android.users;

import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.FollowableItem;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.presentation.UpdatableItem;
import com.soundcloud.android.search.SearchableItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

import android.os.Parcel;
import android.os.Parcelable;

public class UserItem implements ListItem, UpdatableItem, FollowableItem, SearchableItem {

    protected final PropertySet source;

    public static UserItem from(PropertySet source) {
        return new UserItem(source);
    }

    public static UserItem from(PublicApiUser publicApiUser) {
        final PropertySet propertySet = PropertySet.from(
                UserProperty.URN.bind(publicApiUser.getUrn()),
                UserProperty.USERNAME.bind(publicApiUser.getUsername()),
                UserProperty.FOLLOWERS_COUNT.bind(publicApiUser.getFollowersCount()),
                UserProperty.ID.bind(publicApiUser.getId()),
                UserProperty.IMAGE_URL_TEMPLATE.bind(publicApiUser.getImageUrlTemplate())
        );
        if (publicApiUser.getCountry() != null) {
            propertySet.put(UserProperty.COUNTRY, publicApiUser.getCountry());
        }

        return new UserItem(propertySet);
    }

    public static UserItem from(ApiUser apiUser) {
        final PropertySet bindings = PropertySet.from(
                UserProperty.URN.bind(apiUser.getUrn()),
                UserProperty.USERNAME.bind(apiUser.getUsername()),
                UserProperty.FOLLOWERS_COUNT.bind(apiUser.getFollowersCount()),
                UserProperty.IMAGE_URL_TEMPLATE.bind(apiUser.getAvatarUrlTemplate())
        );
        // this should be modeled with an Option type instead:
        // https://github.com/soundcloud/propeller/issues/32
        if (apiUser.getCountry() != null) {
            bindings.put(UserProperty.COUNTRY, apiUser.getCountry());
        }

        return new UserItem(bindings);
    }

    public static Func1<PropertySet, UserItem> fromPropertySet() {
        return bindings -> UserItem.from(bindings);
    }

    UserItem(PropertySet source) {
        this.source = source;
    }

    @Override
    public UserItem updated(PropertySet sourceSet) {
        this.source.update(sourceSet);
        return this;
    }

    @Override
    public FollowableItem updatedWithFollowing(boolean isFollowedByMe, int followingsCount) {
        this.source.put(UserProperty.IS_FOLLOWED_BY_ME, isFollowedByMe);
        this.source.put(UserProperty.FOLLOWERS_COUNT, followingsCount);
        return this;
    }

    public EntityStateChangedEvent toEntityStateChangedEvent() {
        return EntityStateChangedEvent.forUpdate(source);
    }

    @Override
    public Urn getUrn() {
        return source.get(UserProperty.URN);
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return source.getOrElse(UserProperty.IMAGE_URL_TEMPLATE, Optional.<String>absent());
    }

    public String getName() {
        return source.get(UserProperty.USERNAME);
    }

    public Optional<String> getCountry() {
        return source.contains(UserProperty.COUNTRY) ?
               Optional.of(source.get(UserProperty.COUNTRY)) : Optional.<String>absent();
    }

    public int getFollowersCount() {
        return source.get(UserProperty.FOLLOWERS_COUNT);
    }

    public boolean isFollowedByMe() {
        return source.getOrElse(UserProperty.IS_FOLLOWED_BY_ME, false);
    }

    public void setFollowing(boolean following) {
        source.put(UserProperty.IS_FOLLOWED_BY_ME, following);
    }

    public EntityStateChangedEvent toUpdateEvent() {
        return EntityStateChangedEvent.forUpdate(source);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(source, 0);
    }

    public UserItem(Parcel in) {
        this.source = in.readParcelable(UserItem.class.getClassLoader());
    }

    public static final Parcelable.Creator<UserItem> CREATOR = new Parcelable.Creator<UserItem>() {
        public UserItem createFromParcel(Parcel in) {
            return new UserItem(in);
        }

        public UserItem[] newArray(int size) {
            return new UserItem[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        return o instanceof UserItem && ((UserItem) o).source.equals(this.source);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(source);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).addValue(source).toString();
    }

}
