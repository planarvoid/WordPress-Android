package com.soundcloud.android.stream;

import static com.soundcloud.android.tracks.OverflowMenuOptions.builder;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

class StreamPlaylistItemRenderer implements CellRenderer<StreamItem.Playlist> {

    private final Resources resources;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final StreamCardViewPresenter cardViewPresenter;
    private final CardEngagementsPresenter cardEngagementsPresenter;

    @Inject
    public StreamPlaylistItemRenderer(PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                      StreamCardViewPresenter cardViewPresenter,
                                      CardEngagementsPresenter cardEngagementsPresenter,
                                      Resources resources) {
        this.cardEngagementsPresenter = cardEngagementsPresenter;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.cardViewPresenter = cardViewPresenter;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView = LayoutInflater.from(parent.getContext())
                                                .inflate(R.layout.stream_playlist_card, parent, false);
        inflatedView.setTag(new StreamPlaylistViewHolder(inflatedView));
        return inflatedView;
    }

    @Override
    public void bindItemView(int position, View view, List<StreamItem.Playlist> playlistItems) {
        final PlaylistItem playlistItem = playlistItems.get(position).playlistItem();
        StreamPlaylistViewHolder itemView = (StreamPlaylistViewHolder) view.getTag();
        itemView.resetAdditionalInformation();

        cardViewPresenter.bind(itemView, playlistItem, getEventContextMetadataBuilder(playlistItem, position));
        showTrackCount(itemView, playlistItem);
        setupEngagementBar(itemView, playlistItem, position);
    }

    private void showTrackCount(StreamPlaylistViewHolder itemView, PlaylistItem playlistItem) {
        String trackString = resources.getQuantityString(R.plurals.number_of_tracks, playlistItem.getTrackCount());
        itemView.setTrackCount(String.valueOf(playlistItem.getTrackCount()), trackString);
    }

    private void setupEngagementBar(StreamPlaylistViewHolder playlistView,
                                    final PlaylistItem playlistItem,
                                    final int position) {

        cardEngagementsPresenter.bind(playlistView,
                                      playlistItem,
                                      getEventContextMetadataBuilder(playlistItem, position).build());

        playlistView.showDuration(ScTextUtils.formatTimestamp(playlistItem.getDuration(), TimeUnit.MILLISECONDS));
        playlistView.setOverflowListener(new StreamItemViewHolder.OverflowListener() {
            @Override
            public void onOverflow(View overflowButton) {
                playlistItemMenuPresenter.show(overflowButton, playlistItem, builder().build(),
                                               getEventContextMetadataBuilder(playlistItem, position));
            }
        });
    }

    @VisibleForTesting
    EventContextMetadata.Builder getEventContextMetadataBuilder(PlaylistItem playlistItem, int position) {
        return EventContextMetadata.builder().invokerScreen(ScreenElement.LIST.get())
                                   .contextScreen(Screen.STREAM.get())
                                   .module(Module.create(Module.STREAM, position))
                                   .pageName(Screen.STREAM.get())
                                   .attributingActivity(AttributingActivity.fromPlayableItem(playlistItem));
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
