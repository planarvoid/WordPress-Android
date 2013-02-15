package com.soundcloud.android.adapter;

import com.soundcloud.android.model.Playable;

import android.net.Uri;

public interface PlayableAdapter {
    public Uri getPlayableUri();
    public Playable getPlayable(int position);
}
