package com.soundcloud.android.likes;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.DefaultFragment;

import android.annotation.SuppressLint;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class TrackLikesFragment extends DefaultFragment {

    @Inject LikeOperations operations;

    public TrackLikesFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }
}
