package com.soundcloud.android.stream;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.experiments.ChangeLikeToSaveExperiment;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.ItemMenuOptions;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter;

import android.content.res.Resources;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class StreamPlaylistItemRenderer implements CellRenderer<PlaylistStreamItem> {

    private final Resources resources;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final StreamCardViewPresenter cardViewPresenter;
    private final CardEngagementsPresenter cardEngagementsPresenter;
    private final ChangeLikeToSaveExperiment changeLikeToSaveExperiment;

    @Inject
    public StreamPlaylistItemRenderer(PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                      StreamCardViewPresenter cardViewPresenter,
                                      CardEngagementsPresenter cardEngagementsPresenter,
                                      Resources resources,
                                      ChangeLikeToSaveExperiment changeLikeToSaveExperiment) {
        this.cardEngagementsPresenter = cardEngagementsPresenter;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.cardViewPresenter = cardViewPresenter;
        this.resources = resources;
        this.changeLikeToSaveExperiment = changeLikeToSaveExperiment;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView = LayoutInflater.from(parent.getContext())
                                                .inflate(R.layout.stream_playlist_card, parent, false);
        inflatedView.setTag(new StreamPlaylistViewHolder(inflatedView, changeLikeToSaveExperiment.isEnabled()));
        return inflatedView;
    }

    @Override
    public void bindItemView(int position, View view, List<PlaylistStreamItem> playlistItems) {
        final PlaylistStreamItem playlistStreamItem = playlistItems.get(position);
        final PlaylistItem playlistItem = playlistStreamItem.getPlaylistItem();
        StreamPlaylistViewHolder itemView = (StreamPlaylistViewHolder) view.getTag();
        itemView.resetAdditionalInformation();

        cardViewPresenter.bind(itemView, playlistItem, getEventContextMetadataBuilder(playlistItem, position), playlistStreamItem.getCreatedAt(), playlistStreamItem.getAvatarUrlTemplate());
        showTrackCount(itemView, playlistItem);
        setupEngagementBar(itemView, playlistItem, position);
    }

    private void showTrackCount(StreamPlaylistViewHolder itemView, PlaylistItem playlistItem) {
        String trackString = resources.getQuantityString(R.plurals.number_of_tracks, playlistItem.trackCount());
        itemView.setTrackCount(String.valueOf(playlistItem.trackCount()), trackString);
    }

    private void setupEngagementBar(StreamPlaylistViewHolder playlistView,
                                    final PlaylistItem playlistItem,
                                    final int position) {

        cardEngagementsPresenter.bind(playlistView,
                                      playlistItem,
                                      getEventContextMetadataBuilder(playlistItem, position).build());

        playlistView.setOverflowListener(overflowButton -> playlistItemMenuPresenter.show(overflowButton,
                                                                                          playlistItem,
                                                                                          getEventContextMetadataBuilder(playlistItem, position),
                                                                                          ItemMenuOptions.Companion.createDefault()));
    }

    @VisibleForTesting
    EventContextMetadata.Builder getEventContextMetadataBuilder(PlaylistItem playlistItem, int position) {
        return EventContextMetadata.builder()
                                   .module(Module.create(Module.STREAM, position))
                                   .pageName(Screen.STREAM.get())
                                   .attributingActivity(AttributingActivity.fromPlayableItem(playlistItem));
    }

    static class StreamPlaylistViewHolder extends StreamItemViewHolder {

        private final TextView trackCount;
        private final TextView tracksView;

        public StreamPlaylistViewHolder(View view, boolean selected) {
            super(view, selected);
            trackCount = ButterKnife.findById(view, R.id.track_count);
            tracksView = ButterKnife.findById(view, R.id.tracks_text);
        }

        public void setTrackCount(String numberOfTracks, String tracksString) {
            this.trackCount.setText(numberOfTracks);
            this.tracksView.setText(tracksString);
        }
    }
}
