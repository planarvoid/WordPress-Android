package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import rx.functions.Func1;

public class UserItem implements ListItem {

    protected final PropertySet source;

    public static UserItem from(PropertySet source) {
        return new UserItem(source);
    }

    public static Func1<PropertySet, UserItem> fromPropertySet() {
        return new Func1<PropertySet, UserItem>() {
            @Override
            public UserItem call(PropertySet bindings) {
                return UserItem.from(bindings);
            }
        };
    }

    UserItem(PropertySet source) {
        this.source = source;
    }

    @Override
    public UserItem update(PropertySet sourceSet) {
        this.source.update(sourceSet);
        return this;
    }

    @Override
    public Urn getUrn() {
        return source.get(UserProperty.URN);
    }

    @Override
    public Optional<String> getArtworkUrlTemplate() {
        return source.get(UserProperty.ARTWORK_URL_TEMPLATE);
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
}
