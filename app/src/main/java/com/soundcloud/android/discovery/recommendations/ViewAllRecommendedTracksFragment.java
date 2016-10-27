package com.soundcloud.android.discovery.recommendations;

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

public class ViewAllRecommendedTracksFragment extends LightCycleSupportFragment<ViewAllRecommendedTracksFragment> {
    public static final String TAG = "ViewAllRecommendedTracksTag";

    @Inject @LightCycle ViewAllRecommendedTracksPresenter presenter;

    public ViewAllRecommendedTracksFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.discovery_recycler_view, container, false);
    }
}
