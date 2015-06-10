package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserPostsFragment extends LightCycleSupportFragment implements ProfileFragment {

    static final String USER_URN_KEY = "user_urn_key";
    static final String SCREEN_KEY = "screen_key";
    static final String SEARCH_QUERY_SOURCE_INFO_KEY = "search_query_source_info_key";

    @Inject @LightCycle UserPostsPresenter presenter;
    @Inject FeatureFlags featureFlags;

    public static UserPostsFragment create(Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(USER_URN_KEY, userUrn);
        bundle.putSerializable(SCREEN_KEY, screen);
        bundle.putParcelable(SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);

        UserPostsFragment fragment = new UserPostsFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    public UserPostsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final int profile_recycle_view = featureFlags.isEnabled(Flag.NEW_PROFILE) ? R.layout.new_profile_recycle_view
                : R.layout.profile_recycle_view;
        return inflater.inflate(profile_recycle_view, container, false);
    }

    @Override
    public ScrollableProfileItem getScrollableProfileItem() {
        return presenter.getScrollableItem();
    }

    @Override
    public RefreshableProfileItem getRefreshableItem() {
        return presenter;
    }
}