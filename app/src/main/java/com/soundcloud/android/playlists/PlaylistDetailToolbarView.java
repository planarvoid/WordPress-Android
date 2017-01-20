package com.soundcloud.android.playlists;

import com.google.auto.factory.AutoFactory;
import com.soundcloud.android.R;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;

import android.support.v4.app.Fragment;
import android.view.Menu;
import android.view.MenuItem;

@AutoFactory
class PlaylistDetailToolbarView extends DefaultSupportFragmentLightCycle<Fragment> {

    private final PlaylistDetailsViewListener listener;
    private Optional<Menu> menu = Optional.absent();
    private Optional<PlaylistDetailsMetadata> metadata = Optional.absent();

    PlaylistDetailToolbarView(PlaylistDetailsViewListener listener) {
        this.listener = listener;
    }

    public void setPlaylist(PlaylistDetailsMetadata metadata) {
        this.metadata = Optional.of(metadata);
        bindView();
    }

    void onPrepareOptionsMenu(Menu menu) {
        this.menu = Optional.of(menu);
        bindView();
    }

    @Override
    public boolean onOptionsItemSelected(Fragment fragment, MenuItem item) {
        if (item.getItemId() == R.id.edit_validate) {
            listener.onExitEditMode();
            return true;
        }
        return false;
    }

    private void bindView() {
        if (menu.isPresent() && metadata.isPresent()) {
            menu.get().findItem(R.id.edit_validate).setVisible(metadata.get().isInEditMode());
        }
    }
}
