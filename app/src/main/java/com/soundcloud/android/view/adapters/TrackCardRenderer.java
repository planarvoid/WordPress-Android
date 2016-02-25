package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.stream.StreamItemViewHolder;
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

public class TrackCardRenderer implements CellRenderer<TrackItem> {
    private final CondensedNumberFormatter numberFormatter;
    private final TrackItemMenuPresenter menuPresenter;
    private final CardEngagementsPresenter engagementsPresenter;
    private final ImageOperations imageOperations;
    private final FeatureFlags featureFlags;
    private final FeatureOperations featureOperations;
    private final Navigator navigator;
    private final Resources resources;

    @Inject
    public TrackCardRenderer(CondensedNumberFormatter numberFormatter,
                             TrackItemMenuPresenter menuPresenter,
                             CardEngagementsPresenter engagementsPresenter,
                             ImageOperations imageOperations,
                             FeatureFlags featureFlags,
                             FeatureOperations featureOperations,
                             Navigator navigator,
                             Resources resources) {
        this.numberFormatter = numberFormatter;
        this.menuPresenter = menuPresenter;
        this.engagementsPresenter = engagementsPresenter;
        this.imageOperations = imageOperations;
        this.featureFlags = featureFlags;
        this.featureOperations = featureOperations;
        this.navigator = navigator;
        this.resources = resources;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.default_track_card, parent, false);
        inflatedView.setTag(new TrackCardViewHolder(inflatedView, imageOperations, featureFlags, featureOperations, navigator, resources));
        return inflatedView;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackItem> trackItems) {
        bindTrackCard(trackItems.get(position), itemView, position);
    }

    public void bindTrackCard(final TrackItem track, final View itemView, final int position) {
        final TrackCardViewHolder viewHolder = (TrackCardViewHolder) itemView.getTag();
        viewHolder.resetAdditionalInformation();

        engagementsPresenter.bind(viewHolder, track, getEventContextMetadata());

        viewHolder.bindArtworkView(track);
        showPlayCountOrNowPlaying(viewHolder, track);
        viewHolder.overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View overflowButton) {
                menuPresenter.show((FragmentActivity) itemView.getContext(), viewHolder.overflowButton, track, position);
            }
        });
    }

    private void loadArtwork(StreamItemViewHolder itemView, PlayableItem playableItem) {
        imageOperations.displayInAdapterView(
                playableItem.getEntityUrn(), ApiImageSize.getFullImageSize(resources),
                itemView.getImage());
    }

    private EventContextMetadata getEventContextMetadata() {
        return EventContextMetadata.builder().invokerScreen(ScreenElement.LIST.get())
                .contextScreen(Screen.STREAM.get())
                .pageName(Screen.STREAM.get())
                .build();
    }

    private void showPlayCountOrNowPlaying(TrackCardViewHolder itemView, TrackItem track) {
        if (track.isPlaying()) {
            itemView.showNowPlaying();
        } else {
            showPlayCount(itemView, track);
        }
    }

    private void showPlayCount(TrackCardViewHolder itemView, TrackItem track) {
        if (track.hasPlayCount()) {
            itemView.showPlayCount(numberFormatter.format(track.getPlayCount()));
        }
    }
}
