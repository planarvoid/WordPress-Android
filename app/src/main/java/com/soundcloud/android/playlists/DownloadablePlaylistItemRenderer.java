package com.soundcloud.android.playlists;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.PlaylistItemRenderer;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.View;

import javax.inject.Inject;
import java.util.List;

public class DownloadablePlaylistItemRenderer extends PlaylistItemRenderer {

    private final FeatureOperations featureOperations;

    @Inject
    public DownloadablePlaylistItemRenderer(Resources resources,
                                            ImageOperations imageOperations,
                                            CondensedNumberFormatter numberFormatter,
                                            PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                            FeatureOperations featureOperations,
                                            EventBus eventBus,
                                            ScreenProvider screenProvider,
                                            Navigator navigator) {

        super(resources, imageOperations, numberFormatter, playlistItemMenuPresenter, eventBus, screenProvider, navigator);
        this.featureOperations = featureOperations;
        allowOfflineOptions();
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlistItems) {
        super.bindItemView(position, itemView, playlistItems);
        final PlaylistItem playlistItem = playlistItems.get(position);

        setDownloadProgressIndicator(itemView, playlistItem);
    }

    private void setDownloadProgressIndicator(View itemView, PlaylistItem playlistItem) {
        final DownloadImageView downloadProgressIcon = (DownloadImageView) itemView.findViewById(R.id.item_download_state);

        if (featureOperations.isOfflineContentEnabled()) {
            downloadProgressIcon.setState(playlistItem.getDownloadState());
        } else {
            downloadProgressIcon.setState(OfflineState.NO_OFFLINE);
        }
    }
}
