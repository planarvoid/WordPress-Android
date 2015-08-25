package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserFollowersFragment extends ScrollableProfileFragment  {

    @Inject @LightCycle UserFollowersPresenter presenter;

    public static UserFollowersFragment create(Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        UserFollowersFragment fragment = new UserFollowersFragment();
        fragment.setArguments(ProfileArguments.from(userUrn,screen, searchQuerySourceInfo));
        return fragment;
    }

    public UserFollowersFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{presenter.getRecyclerView(), presenter.getEmptyView()};
    }
}