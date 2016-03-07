package com.soundcloud.android.profile;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class UserSoundsFragment extends ScrollableProfileFragment {
    public static final String IS_CURRENT_USER = "is_current_user";

    @Inject @LightCycle UserSoundsPresenter presenter;

    public static UserSoundsFragment create(Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        UserSoundsFragment fragment = new UserSoundsFragment();
        fragment.setArguments(ProfileArguments.from(userUrn, screen, searchQuerySourceInfo));
        return fragment;
    }

    public static Fragment createForCurrentUser(Urn userUrn, Screen screen, SearchQuerySourceInfo searchQuerySource) {
        UserSoundsFragment fragment = create(userUrn, screen, searchQuerySource);
        final Bundle bundle = fragment.getArguments();
        bundle.putBoolean(IS_CURRENT_USER, true);
        fragment.setArguments(bundle);
        return fragment;
    }

    public UserSoundsFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View fragmentView = inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
        fragmentView.setBackgroundColor(getResources().getColor(R.color.white));
        return fragmentView;
    }

    @Override
    public View[] getRefreshableViews() {
        return new View[]{ presenter.getRecyclerView(), presenter.getEmptyView()};
    }
}
