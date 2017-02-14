package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.utils.ViewUtils.getFragmentActivity;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.util.CondensedNumberFormatter;
import com.soundcloud.java.optional.Optional;

import android.content.res.Resources;
import android.support.annotation.LayoutRes;
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
    private final Navigator navigator;
    private final Resources resources;
    private final ScreenProvider screenProvider;
    private final FeatureFlags flags;
    private int layoutResource = R.layout.default_track_card;

    @Inject
    public TrackCardRenderer(CondensedNumberFormatter numberFormatter,
                             TrackItemMenuPresenter menuPresenter,
                             CardEngagementsPresenter engagementsPresenter,
                             ImageOperations imageOperations,
                             Navigator navigator,
                             Resources resources,
                             ScreenProvider screenProvider,
                             FeatureFlags flags) {
        this.numberFormatter = numberFormatter;
        this.menuPresenter = menuPresenter;
        this.engagementsPresenter = engagementsPresenter;
        this.imageOperations = imageOperations;
        this.navigator = navigator;
        this.resources = resources;
        this.screenProvider = screenProvider;
        this.flags = flags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(layoutResource, parent, false);
        inflatedView.setTag(new TrackCardViewHolder(inflatedView, imageOperations, navigator, resources));
        return inflatedView;
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackItem> trackItems) {
        bindTrackCard(trackItems.get(position), itemView, position, Optional.<Module>absent());
    }

    public void bindTrackCard(final TrackItem track,
                              final View itemView,
                              final int position,
                              final Optional<Module> module) {
        final TrackCardViewHolder viewHolder = (TrackCardViewHolder) itemView.getTag();
        viewHolder.resetAdditionalInformation();

        engagementsPresenter.bind(viewHolder, track, getEventContextMetadataBuilder(module).build());

        viewHolder.bindArtworkView(track, flags.isEnabled(Flag.MID_TIER_ROLLOUT));
        showPlayCountOrNowPlaying(viewHolder, track);
        viewHolder.overflowButton.setOnClickListener(overflowButton -> menuPresenter.show(getFragmentActivity(itemView),
                                                                                  viewHolder.overflowButton,
                                                                                  track,
                                                                                  getEventContextMetadataBuilder(module)));
    }

    public void setLayoutResource(@LayoutRes int layoutResource) {
        this.layoutResource = layoutResource;
    }

    private EventContextMetadata.Builder getEventContextMetadataBuilder(Optional<Module> module) {
        EventContextMetadata.Builder builder = EventContextMetadata.builder()
                                                                   .invokerScreen(ScreenElement.LIST.get())
                                                                   .contextScreen(screenProvider.getLastScreenTag())
                                                                   .pageName(screenProvider.getLastScreenTag());
        if (module.isPresent()) {
            builder.module(module.get());
        }
        return builder;
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
