package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.features.FeatureOperations;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.offline.DownloadState;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.AnimUtils;
import com.soundcloud.android.view.adapters.PlaylistItemPresenter;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;

import javax.inject.Inject;
import java.util.List;

public class DownloadablePlaylistItemPresenter extends PlaylistItemPresenter {

    private final FeatureOperations featureOperations;

    @Inject
    public DownloadablePlaylistItemPresenter(Resources resources,
                                             ImageOperations imageOperations,
                                             PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                             FeatureFlags featureFlags,
                                             FeatureOperations featureOperations) {
        super(resources, imageOperations, playlistItemMenuPresenter, featureFlags);
        this.featureOperations = featureOperations;
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlistItems) {
        super.bindItemView(position, itemView, playlistItems);
        final PlaylistItem playlistItem = playlistItems.get(position);

        setDownloadProgressIndicator(itemView, playlistItem);
    }

    private void setDownloadProgressIndicator(View itemView, PlaylistItem playlistItem) {
        final ImageView downloadProgressIcon = (ImageView) itemView.findViewById(R.id.download_progress_icon);
        downloadProgressIcon.clearAnimation();

        if (featureOperations.isOfflineContentEnabled()) {
            final DownloadState downloadState = playlistItem.getDownloadState();
            switch (downloadState) {
                case NO_OFFLINE:
                    setNoOfflineState(downloadProgressIcon);
                    break;
                case REQUESTED:
                    setRequestedDownloadState(downloadProgressIcon);
                    break;
                case DOWNLOADING:
                    setDownloadingState(itemView, downloadProgressIcon);
                    break;
                case DOWNLOADED:
                    setDownloadedState(downloadProgressIcon);
                    break;
                default:
                    throw new IllegalStateException("Playlist download state not expected: " + downloadState);
            }
        } else {
            setNoOfflineState(downloadProgressIcon);
        }
    }

    private void setNoOfflineState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setVisibility(View.GONE);
    }

    private void setRequestedDownloadState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.entity_downloadable);
        downloadProgressIcon.setVisibility(View.VISIBLE);
    }

    private void setDownloadingState(View itemView, ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.entity_downloading);
        downloadProgressIcon.setVisibility(View.VISIBLE);
        AnimUtils.runSpinClockwiseAnimationOn(itemView.getContext(), downloadProgressIcon);
    }

    private void setDownloadedState(ImageView downloadProgressIcon) {
        downloadProgressIcon.setImageResource(R.drawable.entity_downloaded);
        downloadProgressIcon.setVisibility(View.VISIBLE);
    }
}
