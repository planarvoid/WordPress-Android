package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.util.CondensedNumberFormatter;

import android.content.res.Resources;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class StreamTrackItemRenderer implements CellRenderer<TrackItem> {

    private final ImageOperations imageOperations;
    private final CondensedNumberFormatter numberFormatter;
    private final TrackItemMenuPresenter trackItemMenuPresenter;
    private final StreamItemHeaderViewPresenter headerViewPresenter;
    private final StreamItemEngagementsPresenter engagementsPresenter;
    private final Resources resources;

    @Inject
    public StreamTrackItemRenderer(ImageOperations imageOperations,
                                   CondensedNumberFormatter numberFormatter,
                                   TrackItemMenuPresenter trackItemMenuPresenter,
                                   StreamItemEngagementsPresenter engagementsPresenter,
                                   StreamItemHeaderViewPresenter headerViewPresenter, Resources resources) {
        this.imageOperations = imageOperations;
        this.numberFormatter = numberFormatter;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.headerViewPresenter = headerViewPresenter;
        this.engagementsPresenter = engagementsPresenter;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.stream_track_item, parent, false);
        inflatedView.setTag(new StreamItemViewHolder(inflatedView));
        return inflatedView;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackItem> trackItems) {
        final TrackItem track = trackItems.get(position);
        StreamItemViewHolder trackView = (StreamItemViewHolder) itemView.getTag();

        headerViewPresenter.setupHeaderView(trackView, track);
        setupArtworkView(trackView, track);
        setupEngagementBar(trackView, track, position);
    }

    private void setupEngagementBar(StreamItemViewHolder trackView, final TrackItem track, final int position) {
        engagementsPresenter.bind(trackView, track);

        showPlayCountOrNowPlaying(trackView, track);
        trackView.setOverflowListener(new StreamItemViewHolder.OverflowListener() {
            @Override
            public void onOverflow(View view) {
                trackItemMenuPresenter.show((FragmentActivity) view.getContext(), view, track, position);
            }
        });
    }

    private void showPlayCountOrNowPlaying(StreamItemViewHolder itemView, TrackItem track) {
        if (track.isPlaying()) {
            itemView.showNowPlaying();
        } else {
            showPlayCount(itemView, track);
        }
    }

    private void showPlayCount(StreamItemViewHolder itemView, TrackItem track) {
        if (track.hasPlayCount()) {
            itemView.showPlayCount(numberFormatter.format(track.getPlayCount()));
        }
    }

    private void setupArtworkView(StreamItemViewHolder trackView, TrackItem trackItem) {
        loadArtwork(trackView, trackItem);
        trackView.setTitle(trackItem.getTitle());
        trackView.setCreator(trackItem.getCreatorName());
    }

    private void loadArtwork(StreamItemViewHolder itemView, TrackItem track) {
        imageOperations.displayInAdapterView(
                track.getEntityUrn(), ApiImageSize.getFullImageSize(resources),
                itemView.getImage());
    }
}
