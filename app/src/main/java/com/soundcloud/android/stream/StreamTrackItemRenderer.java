package com.soundcloud.android.stream;

import com.soundcloud.android.R;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.OverflowMenuOptions;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.util.CondensedNumberFormatter;

import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class StreamTrackItemRenderer implements CellRenderer<TrackItem> {

    private final CondensedNumberFormatter numberFormatter;
    private final TrackItemMenuPresenter menuPresenter;
    private final StreamCardViewPresenter cardViewPresenter;
    private final StreamItemEngagementsPresenter engagementsPresenter;

    @Inject
    public StreamTrackItemRenderer(CondensedNumberFormatter numberFormatter,
                                   TrackItemMenuPresenter menuPresenter,
                                   StreamItemEngagementsPresenter engagementsPresenter,
                                   StreamCardViewPresenter cardViewPresenter) {
        this.numberFormatter = numberFormatter;
        this.menuPresenter = menuPresenter;
        this.cardViewPresenter = cardViewPresenter;
        this.engagementsPresenter = engagementsPresenter;
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

        cardViewPresenter.bind(trackView, track);
        engagementsPresenter.bind(trackView, track);

        showPlayCountOrNowPlaying(trackView, track);
        trackView.setOverflowListener(new StreamItemViewHolder.OverflowListener() {
            @Override
            public void onOverflow(View view) {
                menuPresenter.show((FragmentActivity) view.getContext(), view, track, position, getOverflowMenuOptions());
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

    private OverflowMenuOptions getOverflowMenuOptions() {
        return OverflowMenuOptions.builder().build();
    }
}
