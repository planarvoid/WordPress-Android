package com.soundcloud.android.search.topresults;

import com.google.auto.value.AutoValue;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.users.UserItem;

@AutoValue
abstract class UserItemClick {
    abstract SearchQuerySourceInfo searchQuerySourceInfo();
    abstract UserItem userItem();
    static UserItemClick create(SearchQuerySourceInfo searchQuerySourceInfo, UserItem userItem) {
        return new AutoValue_UserItemClick(searchQuerySourceInfo, userItem);
    }
}
