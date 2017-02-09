package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.R;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.view.Menu;

@AutoFactory
class PlaylistDetailToolbarView extends DefaultSupportFragmentLightCycle<Fragment> {

    private final Resources resources;
    private final PlaylistDetailsInputs listener;
    private ActionBar actionBar;
    private Optional<Menu> menu = Optional.absent();
    private Optional<PlaylistDetailsMetadata> metadata = Optional.absent();

    PlaylistDetailToolbarView(@Provided Resources resources, PlaylistDetailsInputs listener, ActionBar actionBar) {
        this.resources = resources;
        this.listener = listener;
        this.actionBar = actionBar;
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
        final String title = isInEditMode.isInEditMode() ? titleEdit() : isInEditMode.label();
        actionBar.setTitle(title);
    }

    private String titleEdit() {
        return resources.getString(R.string.edit_playlist_title);
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        actionBar = null;
        menu = Optional.absent();
    }
}
