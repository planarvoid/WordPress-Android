package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUICommand;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

class PlaylistContentPresenter extends DefaultSupportFragmentLightCycle<Fragment> {
    private final PlaylistEditViewFactory playlistEditViewFactory;
    private final PlaylistDefaultViewFactory playlistDefaultViewFactory;

    private PlaylistContentView playlistContentView = PlaylistContentView.EMPTY;

    @Inject
    PlaylistContentPresenter(PlaylistEditViewFactory playlistEditViewFactory,
                             PlaylistDefaultViewFactory playlistDefaultViewFactory) {
        this.playlistEditViewFactory = playlistEditViewFactory;
        this.playlistDefaultViewFactory = playlistDefaultViewFactory;
    }

    @Override
    public void onStart(Fragment fragment) {
        super.onStart(fragment);
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        playlistContentView.start();
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        playlistContentView.stop();
    }

    public void setEditMode(PlaylistPresenter playlistPresenter, boolean isEditMode) {
        if (isEditMode) {
            switchViewMode(playlistEditViewFactory.create(playlistPresenter));
        } else {
            switchViewMode(playlistDefaultViewFactory.create(playlistPresenter));
        }
    }

    public void onItemClicked(int position) {
        playlistContentView.onItemClicked(position);
    }

    private void switchViewMode(PlaylistContentView newView) {
        playlistContentView.stop();
        playlistContentView = newView;
        playlistContentView.start();
    }

    interface PlaylistContentView {

        PlaylistContentView EMPTY = new PlaylistContentView() {
            @Override
            public void start() {
            }

            @Override
            public void stop() {
            }

            @Override
            public void onItemClicked(int position) {
            }

            @Override
            public void onHeaderClick() {
            }
        };

        void start();

        void stop();

        void onItemClicked(int position);

        void onHeaderClick();
    }

}
