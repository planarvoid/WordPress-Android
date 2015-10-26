package com.soundcloud.android.stream;

import butterknife.ButterKnife;
import com.appboy.ui.support.StringUtils;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.utils.ScTextUtils;

import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

class StreamPlaylistItemRenderer implements CellRenderer<PlaylistItem> {

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final Resources resources;
    private final StreamItemHeaderViewPresenter headerViewPresenter;

    @Inject
    public StreamPlaylistItemRenderer(ImageOperations imageOperations,
                                      CondensedNumberFormatter numberFormatter,
                                      PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                      Resources resources, StreamItemHeaderViewPresenter headerViewPresenter) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.resources = resources;
        this.headerViewPresenter = headerViewPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.stream_playlist_item, parent, false);
        inflatedView.setTag(new StreamPlaylistViewHolder(inflatedView));
        return inflatedView;
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistItem> playlistItems) {
        final PlaylistItem playlistItem = playlistItems.get(position);
        StreamPlaylistViewHolder itemView = (StreamPlaylistViewHolder) view.getTag();

        headerViewPresenter.setupHeaderView(itemView, playlistItem);
        setupArtworkView(itemView, playlistItem);
        setupEngagementBar(itemView, playlistItem);
    }

    private void setupArtworkView(StreamPlaylistViewHolder itemView, PlaylistItem playlistItem) {
        loadArtwork(itemView, playlistItem);
        itemView.setTitle(playlistItem.getTitle());
        itemView.setCreator(playlistItem.getCreatorName());
        showTrackCount(itemView, playlistItem);
    }

    private void showTrackCount(StreamPlaylistViewHolder itemView, PlaylistItem playlistItem) {
        String trackString = resources.getQuantityString(R.plurals.number_of_tracks, playlistItem.getTrackCount());
        itemView.setTrackCount(String.valueOf(playlistItem.getTrackCount()), trackString);
    }

    private void setupEngagementBar(StreamPlaylistViewHolder playlistView, final PlaylistItem playlistItem) {
        playlistView.resetAdditionalInformation();

        playlistView.showDuration(ScTextUtils.formatTimestamp(playlistItem.getDuration(), TimeUnit.MILLISECONDS));
        playlistView.showLikeStats(getCountString(playlistItem.getLikesCount()), playlistItem.isLiked());

        playlistView.showRepostStats(getCountString(playlistItem.getRepostCount()), playlistItem.isReposted());
        playlistView.setOverflowListener(new StreamPlaylistViewHolder.OverflowListener() {
            @Override
            public void onOverflow(View overflowButton) {
                playlistItemMenuPresenter.show(overflowButton, playlistItem, false);
            }
        });
    }

    private String getCountString(int count) {
        if (count > 0) {
            return numberFormatter.format(count);
        } else {
            return StringUtils.EMPTY_STRING;
        }
    }

    protected void loadArtwork(StreamPlaylistViewHolder itemView, PlaylistItem playlistItem) {
        imageOperations.displayInAdapterView(
                playlistItem.getEntityUrn(), ApiImageSize.getFullImageSize(resources),
                itemView.getImage());
    }

    static class StreamPlaylistViewHolder extends StreamItemViewHolder {

        private final TextView trackCount;
        private final TextView tracksView;

        public StreamPlaylistViewHolder(View view) {
            super(view);
            trackCount = ButterKnife.findById(view, R.id.track_count);
            tracksView = ButterKnife.findById(view, R.id.tracks_text);
        }

        public void setTrackCount(String numberOfTracks, String tracksString) {
            this.trackCount.setText(numberOfTracks);
            this.tracksView.setText(tracksString);
        }
    }
}
