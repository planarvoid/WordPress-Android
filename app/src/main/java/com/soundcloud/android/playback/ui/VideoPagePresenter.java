package com.soundcloud.android.playback.ui;

import com.soundcloud.android.R;
import com.soundcloud.android.events.EntityStateChangedEvent;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.Player;

import android.content.Context;
import android.content.res.Resources;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

class VideoPagePresenter implements PlayerPagePresenter<PlayerAd> {

    private final Resources resources;
    private final Context context;

    @Inject
    public VideoPagePresenter(Resources resources, Context context) {
        this.resources = resources;
        this.context = context;
    }

    @Override
    public View createItemView(ViewGroup container, SkipListener skipListener) {
        SurfaceView view = new SurfaceView(context);
        view.setBackgroundColor(resources.getColor(R.color.sc_orange));
        return view;
    }

    @Override
    public View clearItemView(View convertView) {
        return convertView;
    }

    @Override
    public void bindItemView(View view, PlayerAd playerItem) {}

    @Override
    public void setProgress(View trackPage, PlaybackProgress progress) {}

    @Override
    public void setPlayState(View trackPage, Player.StateTransition stateTransition, boolean isCurrentTrack, boolean isForeground) {}

    @Override
    public void onPlayableUpdated(View trackPage, EntityStateChangedEvent trackChangedEvent) {}

    @Override
    public void onBackground(View trackPage) {}

    @Override
    public void onForeground(View trackPage) {}

    @Override
    public void onDestroyView(View trackPage) {}

    @Override
    public void setCollapsed(View trackPage) {}

    @Override
    public void setExpanded(View trackPage) {}

    @Override
    public void onPlayerSlide(View trackPage, float position) {}

    @Override
    public void clearAdOverlay(View trackPage) {}

    @Override
    public void setCastDeviceName(View trackPage, String deviceName) {}
}
