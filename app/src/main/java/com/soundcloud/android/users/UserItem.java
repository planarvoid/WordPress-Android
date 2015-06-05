package com.soundcloud.android.users;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.ListItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.propeller.PropertySet;

public class UserItem implements ListItem {

    protected final PropertySet source;

    public static UserItem from(PropertySet source) {
        return new UserItem(source);
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
    public Urn getEntityUrn() {
        return source.get(UserProperty.URN);
    }

    public String getName() {
        return source.get(UserProperty.USERNAME);
    }

    public String getCountry() {
        return source.getOrElse(UserProperty.COUNTRY, ScTextUtils.EMPTY_STRING);
    }

    public int getFollowersCount() {
        return source.get(UserProperty.FOLLOWERS_COUNT);
    }

    public boolean isFollowedByMe() {
        return source.getOrElse(UserProperty.IS_FOLLOWED_BY_ME, false);
    }
}
