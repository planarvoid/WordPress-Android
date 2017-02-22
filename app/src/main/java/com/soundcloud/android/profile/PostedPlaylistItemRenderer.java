package com.soundcloud.android.profile;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.View;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class PostedPlaylistItemRenderer extends PlaylistItemRenderer {

    @Inject
    public PostedPlaylistItemRenderer(Resources resources,
                                      ImageOperations imageOperations,
                                      CondensedNumberFormatter numberFormatter,
                                      PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                      EventBus eventBus, ScreenProvider screenProvider,
                                      Navigator navigator) {
        super(resources,
              imageOperations,
              numberFormatter,
              playlistItemMenuPresenter,
              eventBus,
              screenProvider,
              navigator);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlists) {
        super.bindItemView(position, itemView, playlists);

        final PlaylistItem playlist = playlists.get(position);
        showReposter(itemView, playlist);
    }

    private void showReposter(View itemView, PlaylistItem playlist) {
        final TextView reposterView = getTextView(itemView, R.id.reposter);
        final Optional<String> optionalReposter = playlist.reposter();
        if (optionalReposter.isPresent()) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(optionalReposter.get());
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }
}
