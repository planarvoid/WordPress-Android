package com.soundcloud.android.profile;

import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;

import android.os.Bundle;

final class ProfileArguments {
    public static final String USER_URN_KEY = "user_urn_key";
    public static final String SCREEN_KEY = "screen_key";
    public static final String SEARCH_QUERY_SOURCE_INFO_KEY = "search_query_source_info_key";

    static Bundle from(Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(USER_URN_KEY, userUrn);
        bundle.putSerializable(SCREEN_KEY, screen);
        bundle.putParcelable(SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        return bundle;
    }
}
