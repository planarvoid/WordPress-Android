package com.soundcloud.android.likes;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.lightcycle.LightCycleSupportFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

public class TrackLikesFragment extends LightCycleSupportFragment {

    @Inject TrackLikesPresenter presenter;

    public TrackLikesFragment() {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        addLifeCycleComponent(presenter);
        //addLifeCycleComponent(adapter.getLifeCycleHandler());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list_with_refresh, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        presenter.onCreateOptionsMenu(menu, inflater);
    }
}
