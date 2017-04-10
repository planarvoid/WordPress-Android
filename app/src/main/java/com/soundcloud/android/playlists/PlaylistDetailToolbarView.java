package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.View;

import javax.inject.Inject;

class PlaylistDetailToolbarView extends DefaultSupportFragmentLightCycle<Fragment> {
    private final BaseLayoutHelper baseLayoutHelper;

    private Optional<Menu> menu = Optional.absent();
    private Optional<PlaylistDetailsMetadata> metadata = Optional.absent();

    private ActionBar actionBar;
    private String titleEdit;

    @Inject
    PlaylistDetailToolbarView(BaseLayoutHelper baseLayoutHelper) {
        this.baseLayoutHelper = baseLayoutHelper;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, Bundle savedInstanceState) {
        baseLayoutHelper.setupActionBar(((AppCompatActivity) fragment.getActivity()));
        titleEdit = fragment.getString(R.string.edit_playlist_title);
        actionBar = ((AppCompatActivity) fragment.getActivity()).getSupportActionBar();
    }


    public void setPlaylist(PlaylistDetailsMetadata metadata) {
        this.metadata = Optional.of(metadata);
        bindView();
    }

    void onPrepareOptionsMenu(Menu menu) {
        this.menu = Optional.of(menu);
        bindView();
    }

    private void bindView() {
        if (menu.isPresent() && metadata.isPresent()) {
            final PlaylistDetailsMetadata playlistDetailsMetadata = metadata.get();
            final boolean isInEditMode = playlistDetailsMetadata.isInEditMode();
            menu.get().findItem(R.id.edit_validate).setVisible(isInEditMode);
            setTitle(playlistDetailsMetadata);
        }
    }

    private void setTitle(PlaylistDetailsMetadata isInEditMode) {
        final String title = isInEditMode.isInEditMode() ? titleEdit : isInEditMode.label();
        actionBar.setTitle(title);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        actionBar = null;
        titleEdit = null;
        menu = Optional.absent();
    }
}
