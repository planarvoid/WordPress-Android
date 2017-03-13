package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class OldUserDetailsFragment extends ScrollableProfileFragment {

    @Inject OldUserDetailsView oldUserDetailsView;
    @Inject UserProfileOperations profileOperations;
    @LightCycle OldUserDetailsPresenter oldUserDetailsPresenter;

    public static OldUserDetailsFragment create(Urn userUrn) {
        final OldUserDetailsFragment oldUserDetailsFragment = new OldUserDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(ProfileArguments.USER_URN_KEY, userUrn);
        oldUserDetailsFragment.setArguments(args);
        return oldUserDetailsFragment;
    }

    public OldUserDetailsFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
        oldUserDetailsPresenter = new OldUserDetailsPresenter(profileOperations, oldUserDetailsView);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        oldUserDetailsView.setUrn(getArguments().getParcelable(ProfileArguments.USER_URN_KEY));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.old_user_info_view, container, false);
    }

    @Override
    public View[] getRefreshableViews() {
        final View view = getView();
        return new View[]{view.findViewById(android.R.id.empty),
                view.findViewById(R.id.user_details_holder)};
    }
}
