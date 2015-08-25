package com.soundcloud.android.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class RecommendedTracksFragment extends LightCycleSupportFragment {

    private static final String EXTRA_LOCAL_SEED_ID = "localSeedId";

    @Inject @LightCycle RecommendedTracksPresenter recommendedTracksPresenter;

    public RecommendedTracksFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    static RecommendedTracksFragment create(long localSeedId) {
        final Bundle bundle = new Bundle();
        bundle.putLong(EXTRA_LOCAL_SEED_ID, localSeedId);
        RecommendedTracksFragment fragment = new RecommendedTracksFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_recyclerview_with_refresh, container, false);
    }
}
