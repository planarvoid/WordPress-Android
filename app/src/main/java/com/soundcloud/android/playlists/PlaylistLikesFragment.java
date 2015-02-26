package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.lightcycle.LightCycleSupportFragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class PlaylistLikesFragment extends LightCycleSupportFragment {

    @Inject PlaylistLikesPresenter presenter;

    public PlaylistLikesFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponent(presenter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list_with_refresh, container, false);
    }
}
