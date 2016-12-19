package com.soundcloud.android.stream;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.events.AttributingActivity;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter;

import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class StreamTrackItemRenderer implements CellRenderer<TrackStreamItem> {

    private final CondensedNumberFormatter numberFormatter;
    private final TrackItemMenuPresenter menuPresenter;
    private final StreamCardViewPresenter streamCardViewPresenter;
    private final CardEngagementsPresenter engagementsPresenter;

    @Inject
    public StreamTrackItemRenderer(CondensedNumberFormatter numberFormatter,
                                   TrackItemMenuPresenter menuPresenter,
                                   CardEngagementsPresenter engagementsPresenter,
                                   StreamCardViewPresenter streamCardViewPresenter) {
        this.numberFormatter = numberFormatter;
        this.menuPresenter = menuPresenter;
        this.streamCardViewPresenter = streamCardViewPresenter;
        this.engagementsPresenter = engagementsPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView = LayoutInflater.from(parent.getContext())
                                                .inflate(R.layout.stream_track_card, parent, false);
        inflatedView.setTag(new StreamItemViewHolder(inflatedView));
        return inflatedView;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackStreamItem> trackItems) {
        final TrackItem track = trackItems.get(position).trackItem();
        StreamItemViewHolder trackView = (StreamItemViewHolder) itemView.getTag();
        trackView.resetAdditionalInformation();

        streamCardViewPresenter.bind(trackView, track, getEventContextMetadataBuilder(track, position));
        engagementsPresenter.bind(trackView, track, getEventContextMetadataBuilder(track, position).build());

        showPlayCountOrNowPlaying(trackView, track);
        trackView.setOverflowListener(view -> menuPresenter.show(getFragmentActivity(view), view, track, position,
                                                         getEventContextMetadataBuilder(track, position)));
    }

    @VisibleForTesting
    EventContextMetadata.Builder getEventContextMetadataBuilder(TrackItem trackItem, Integer position) {
        return EventContextMetadata.builder().invokerScreen(ScreenElement.LIST.get())
                                   .module(Module.create(Module.STREAM, position))
                                   .contextScreen(Screen.STREAM.get())
                                   .pageName(Screen.STREAM.get())
                                   .attributingActivity(AttributingActivity.fromPlayableItem(trackItem));
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

}
