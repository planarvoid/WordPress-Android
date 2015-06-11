package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserDetailsFragment extends LightCycleSupportFragment implements ProfileFragment {

    @Inject UserDetailsView userDetailsView;
    @Inject UserDetailsScroller userDetailsScroller;
    @Inject ProfileOperations profileOperations;
    @LightCycle UserDetailsPresenter userDetailsPresenter;

    public static UserDetailsFragment create() {
        return new UserDetailsFragment();
    }

    public UserDetailsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
        userDetailsPresenter = new UserDetailsPresenter(profileOperations, userDetailsView, userDetailsScroller);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userDetailsView.setUrn(getActivity().getIntent().<Urn>getParcelableExtra(ProfileActivity.EXTRA_USER_URN));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_info_view, container, false);
    }

    @Override
    public ScrollableProfileItem getScrollableProfileItem() {
        return userDetailsScroller;
    }

    @Override
    public RefreshableProfileItem getRefreshableItem() {
        return userDetailsPresenter;
    }
}
