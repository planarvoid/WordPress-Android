package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.playback.service.PlayQueueManager;
import com.soundcloud.android.playback.ui.TrackMenuWrapperListener;
import com.soundcloud.android.playlists.AddToPlaylistDialogFragment;
import com.soundcloud.android.view.menu.PopupMenuWrapper;
import com.soundcloud.propeller.PropertySet;

import android.support.v4.app.FragmentActivity;
import android.view.MenuItem;
import android.view.View;

import javax.inject.Inject;

public final class TrackItemMenuController implements TrackMenuWrapperListener {
    private final PlayQueueManager playQueueManager;
    private final SoundAssociationOperations associationOperations;
    private final PopupMenuWrapper.Factory popupMenuWrapperFactory;

    private PopupMenuWrapper menu;
    private FragmentActivity activity;
    private PropertySet track;

    @Inject
    TrackItemMenuController(PlayQueueManager playQueueManager,
                            SoundAssociationOperations associationOperations,
                            PopupMenuWrapper.Factory popupMenuWrapperFactory) {
        this.playQueueManager = playQueueManager;
        this.associationOperations = associationOperations;
        this.popupMenuWrapperFactory = popupMenuWrapperFactory;
    }

    public void show(FragmentActivity activity, View button, PropertySet track) {
        this.activity = activity;
        this.track = track;
        menu = popupMenuWrapperFactory.build(button.getContext(), button);
        menu.inflate(R.menu.track_item_actions);
        menu.setOnMenuItemClickListener(this);
        menu.setOnDismissListener(this);
        menu.show();
    }

    public void dismiss() {
        menu.dismiss();
    }

    @Override
    public void onDismiss() {
        activity = null;
        track = null;
        menu = null;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.add_to_likes:
                handleLike();
                return true;
            case R.id.add_to_playlist:
                showAddToPlaylistDialog(activity, track);
                return true;
            default:
                return false;
        }
    }

    private void handleLike() {
        // TODO
    }

    private void showAddToPlaylistDialog(FragmentActivity activity, PropertySet track) {
        AddToPlaylistDialogFragment from = AddToPlaylistDialogFragment.from(track, playQueueManager.getScreenTag());
        from.show(activity.getSupportFragmentManager());
    }
}
