package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserFollowersFragment extends LightCycleSupportFragment implements ProfileFragment {

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
        return inflater.inflate(R.layout.new_profile_recycle_view, container, false);
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