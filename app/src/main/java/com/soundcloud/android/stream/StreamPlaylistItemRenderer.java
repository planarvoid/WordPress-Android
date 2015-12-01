package com.soundcloud.android.stream;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.OverflowMenuOptions;
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

    private final Resources resources;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final StreamCardViewPresenter cardViewPresenter;
    private final StreamItemEngagementsPresenter cardEngagementsPresenter;

    @Inject
    public StreamPlaylistItemRenderer(PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                      StreamCardViewPresenter cardViewPresenter,
                                      StreamItemEngagementsPresenter cardEngagementsPresenter,
                                      Resources resources) {
        this.cardEngagementsPresenter = cardEngagementsPresenter;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.cardViewPresenter = cardViewPresenter;
        this.resources = resources;
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

        cardViewPresenter.bind(itemView, playlistItem);
        showTrackCount(itemView, playlistItem);
        setupEngagementBar(itemView, playlistItem);
    }

    private void showTrackCount(StreamPlaylistViewHolder itemView, PlaylistItem playlistItem) {
        String trackString = resources.getQuantityString(R.plurals.number_of_tracks, playlistItem.getTrackCount());
        itemView.setTrackCount(String.valueOf(playlistItem.getTrackCount()), trackString);
    }

    private void setupEngagementBar(StreamPlaylistViewHolder playlistView, final PlaylistItem playlistItem) {
        cardEngagementsPresenter.bind(playlistView, playlistItem);

        playlistView.showDuration(ScTextUtils.formatTimestamp(playlistItem.getDuration(), TimeUnit.MILLISECONDS));
        playlistView.setOverflowListener(new StreamItemViewHolder.OverflowListener() {
            @Override
            public void onOverflow(View overflowButton) {
                playlistItemMenuPresenter.show(overflowButton, playlistItem, getOverflowMenuOptions());
            }
        });
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

    private OverflowMenuOptions getOverflowMenuOptions() {
        return OverflowMenuOptions.builder().build();
    }
}
