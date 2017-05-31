package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.presentation.RefreshableScreen;
import com.soundcloud.android.utils.LeakCanaryWrapper;
import com.soundcloud.android.view.MultiSwipeRefreshLayout;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserDetailsFragment extends LightCycleSupportFragment<UserDetailsFragment>
        implements RefreshableScreen {

    @Inject LeakCanaryWrapper leakCanaryWrapper;
    @Inject UserProfileOperations profileOperations;
    @Inject Navigator navigator;
    @Inject ScreenProvider screenProvider;
    @Inject @LightCycle UserDetailsPresenter userDetailsPresenter;

    public static UserDetailsFragment create(Urn userUrn,
                                             SearchQuerySourceInfo searchQuerySourceInfo) {
        final UserDetailsFragment userDetailsFragment = new UserDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ProfileArguments.USER_URN_KEY, userUrn);
        args.putParcelable(ProfileArguments.SEARCH_QUERY_SOURCE_INFO_KEY, searchQuerySourceInfo);
        userDetailsFragment.setArguments(args);
        return userDetailsFragment;
    }

    public UserDetailsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_info_view, container, false);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        leakCanaryWrapper.watch(this);
    }

    @Override
    public MultiSwipeRefreshLayout getRefreshLayout() {
        return (MultiSwipeRefreshLayout) getView().findViewById(R.id.str_layout);
    }

    @Override
    public View[] getRefreshableViews() {
        final View view = getView();
        return new View[]{view.findViewById(android.R.id.empty),
                view.findViewById(R.id.user_details_holder)};
    }
}
