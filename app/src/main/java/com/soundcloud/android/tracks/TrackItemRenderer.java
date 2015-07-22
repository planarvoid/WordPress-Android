package com.soundcloud.android.tracks;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.optional.Optional;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackItemRenderer implements CellRenderer<TrackItem> {

    private final ImageOperations imageOperations;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;
    protected final FeatureOperations featureOperations;
    private final TrackItemView.Factory trackItemViewFactory;

    protected final TrackItemMenuPresenter trackItemMenuPresenter;

    private Urn playingTrack = Urn.NOT_SET;

    @Inject
    public TrackItemRenderer(ImageOperations imageOperations,
                             TrackItemMenuPresenter trackItemMenuPresenter,
                             EventBus eventBus,
                             ScreenProvider screenProvider,
                             Navigator navigator,
                             FeatureOperations featureOperations,
                             TrackItemView.Factory trackItemViewFactory) {

        this.imageOperations = imageOperations;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
        this.featureOperations = featureOperations;
        this.trackItemViewFactory = trackItemViewFactory;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemViewFactory.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<TrackItem> trackItems) {
        final TrackItem track = trackItems.get(position);
        TrackItemView trackItemView = (TrackItemView) itemView.getTag();
        trackItemView.setCreator(track.getCreatorName());
        trackItemView.setTitle(track.getTitle());
        trackItemView.setDuration(ScTextUtils.formatTimestamp(track.getDuration(), TimeUnit.MILLISECONDS));

        showRelevantAdditionalInformation(trackItemView, track);
        toggleReposterView(trackItemView, track);

        loadArtwork(trackItemView, track);
        setupOverFlow(trackItemView, track, position);
    }

    protected void setupOverFlow(final TrackItemView itemView, final TrackItem track, final int position) {
        itemView.setOverflowListener(new TrackItemView.OverflowListener() {
            @Override
            public void onOverflow(View overflowButton) {
                showTrackItemMenu(overflowButton, track, position);
            }
        });
    }

    protected void showTrackItemMenu(View button, TrackItem track, int position) {
        trackItemMenuPresenter.show((FragmentActivity) button.getContext(), button, track, position);
    }

    private void loadArtwork(TrackItemView itemView, TrackItem track) {
        imageOperations.displayInAdapterView(
                track.getEntityUrn(), ApiImageSize.getListItemImageSize(itemView.getContext()),
                itemView.getImage());
    }

    private void toggleReposterView(TrackItemView itemView, TrackItem track) {
        final Optional<String> optionalReposter = track.getReposter();
        if (optionalReposter.isPresent()) {
            itemView.showReposter(optionalReposter.get());
        } else {
            itemView.hideReposter();
        }
    }

    private void showRelevantAdditionalInformation(TrackItemView itemView, TrackItem track) {
        itemView.resetAdditionalInformation();
        if (track instanceof PromotedTrackItem) {
            showPromoted(itemView, (PromotedTrackItem) track);
        } else if (track.getEntityUrn().equals(playingTrack)) {
            itemView.showNowPlaying();
        } else if (track.isMidTier() && featureOperations.upsellMidTier()) {
            itemView.showUpsell();
        } else if (track.isPrivate()) {
            itemView.showPrivateIndicator();
        } else {
            showPlayCount(itemView, track);
        }
    }

    private void showPromoted(TrackItemView itemView, final PromotedTrackItem track) {
        final Context context = itemView.getContext();
        if (track.hasPromoter()) {
            itemView.showPromotedTrack(context.getString(R.string.promoted_by_label, track.getPromoterName().get()));
            itemView.setPromotedClickable(new PromoterClickViewListener(track, eventBus, screenProvider, navigator));
        } else {
            itemView.showPromotedTrack(context.getString(R.string.promoted_label));
        }
    }

    private void showPlayCount(TrackItemView itemView, TrackItem track) {
        final int playCount = track.getPlayCount();
        if (hasPlayCount(playCount)) {
            itemView.showPlaycount(ScTextUtils.formatNumberWithCommas(playCount));
        }
    }


    private boolean hasPlayCount(int playCount) {
        return playCount > 0;
    }

    public void setPlayingTrack(@NotNull Urn playingTrack) {
        this.playingTrack = playingTrack;
    }

}
