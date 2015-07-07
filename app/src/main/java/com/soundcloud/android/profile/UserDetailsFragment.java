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

public class UserDetailsFragment extends LightCycleSupportFragment {

    @Inject UserDetailsView userDetailsView;
    @Inject UserProfileOperations profileOperations;
    @LightCycle UserDetailsPresenter userDetailsPresenter;

    public static UserDetailsFragment create(Urn userUrn) {
        final UserDetailsFragment userDetailsFragment = new UserDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ProfileArguments.USER_URN_KEY, userUrn);
        userDetailsFragment.setArguments(args);
        return userDetailsFragment;
    }

    public UserDetailsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
        userDetailsPresenter = new UserDetailsPresenter(profileOperations, userDetailsView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userDetailsView.setUrn(getArguments().<Urn>getParcelable(ProfileArguments.USER_URN_KEY));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.user_info_view, container, false);
    }
}
