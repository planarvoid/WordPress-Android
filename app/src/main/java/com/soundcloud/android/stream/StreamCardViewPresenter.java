package com.soundcloud.android.stream;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.tracks.TieredTrack;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.View;

import javax.inject.Inject;
import java.util.Date;

class StreamCardViewPresenter {

    private final HeaderSpannableBuilder headerSpannableBuilder;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;
    private final Resources resources;
    private final ImageOperations imageOperations;

    @Inject
    StreamCardViewPresenter(HeaderSpannableBuilder headerSpannableBuilder, EventBus eventBus,
                            ScreenProvider screenProvider, Navigator navigator, Resources resources,
                            ImageOperations imageOperations) {

        this.headerSpannableBuilder = headerSpannableBuilder;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
        this.resources = resources;
        this.imageOperations = imageOperations;
    }

    void bind(StreamItemViewHolder itemView, PlayableItem item) {
        bindHeaderView(itemView, item);
        bindArtworkView(itemView, item);
    }

    private void bindHeaderView(StreamItemViewHolder itemView, PlayableItem playableItem) {
        itemView.resetCardView();

        if (playableItem instanceof PromotedListItem) {
            showPromoted(itemView, (PromotedListItem) playableItem, playableItem.getPlayableType());
        } else {
            loadAvatar(itemView, playableItem.getUserUrn(), playableItem.getAvatarUrlTemplate());
            setHeaderText(itemView, playableItem);
            showCreatedAt(itemView, playableItem.getCreatedAt());
            itemView.togglePrivateIndicator(playableItem.isPrivate());
        }
    }

    private void bindArtworkView(StreamItemViewHolder itemView, PlayableItem playableItem) {
        loadArtwork(itemView, playableItem);
        itemView.setTitle(playableItem.getTitle());
        itemView.setArtist(playableItem.getCreatorName());
        itemView.setArtistClickable(new ProfileClickViewListener(playableItem.getCreatorUrn()));
        bindHighTierLabel(itemView, playableItem);
    }

    private void bindHighTierLabel(StreamItemViewHolder itemView, PlayableItem playableItem) {
        itemView.hideHighTierLabel();
        if (playableItem instanceof TieredTrack) {
            TieredTrack tieredTrack = ((TieredTrack) playableItem);
            if (isHighTierPreview(tieredTrack)) {
                itemView.showHighTierLabel(R.string.upsell_track_preview);
            } else if (isFullHighTierTrack(tieredTrack)) {
                itemView.showHighTierLabel(R.string.go);
            }
        }
    }

    private void loadArtwork(StreamItemViewHolder itemView, PlayableItem playableItem) {
        imageOperations.displayInAdapterView(
                playableItem, ApiImageSize.getFullImageSize(resources),
                itemView.getImage());
    }

    private void setHeaderText(StreamItemViewHolder itemView, PlayableItem playableItem) {
        boolean isRepost = playableItem.getReposter().isPresent();
        final String userName = playableItem.getReposter().or(playableItem.getCreatorName());
        final String action = resources.getString(isRepost ?
                                                  R.string.stream_reposted_action :
                                                  R.string.stream_posted_action);

        if (isRepost) {
            headerSpannableBuilder.actionSpannedString(action, playableItem.getPlayableType());
            itemView.setRepostHeader(userName, headerSpannableBuilder.get());
        } else {
            headerSpannableBuilder.userActionSpannedString(userName, action, playableItem.getPlayableType());
            itemView.setHeaderText(headerSpannableBuilder.get());
        }
    }

    private void showPromoted(StreamItemViewHolder itemView, PromotedListItem promoted, String playableType) {
        if (promoted.hasPromoter()) {
            final String action = resources.getString(R.string.stream_promoted_action);
            loadAvatar(itemView, promoted.getPromoterUrn().get(), promoted.getAvatarUrlTemplate());
            headerSpannableBuilder.actionSpannedString(action, playableType);
            itemView.setPromoterHeader(promoted.getPromoterName().get(), headerSpannableBuilder.get());
            itemView.setPromoterClickable(new PromoterClickViewListener(promoted, eventBus, screenProvider, navigator));
        } else {
            itemView.hideUserImage();

            headerSpannableBuilder.promotedSpannedString(playableType);
            itemView.setPromotedHeader(headerSpannableBuilder.get());
        }
    }

    private void showCreatedAt(StreamItemViewHolder itemView, Date createdAt) {
        final String formattedTime = ScTextUtils.formatTimeElapsedSince(resources, createdAt.getTime(), true);
        itemView.setCreatedAt(formattedTime);
    }

    private void loadAvatar(StreamItemViewHolder itemView, Urn userUrn, Optional<String> avatarUrl) {
        final ImageResource avatar = SimpleImageResource.create(userUrn, avatarUrl);
        itemView.setCreatorClickable(new ProfileClickViewListener(userUrn));
        imageOperations.displayCircularInAdapterView(
                avatar, ApiImageSize.getListItemImageSize(resources),
                itemView.getUserImage());
    }

    private class ProfileClickViewListener implements View.OnClickListener {

        private final Urn userUrn;

        ProfileClickViewListener(Urn userUrn) {
            this.userUrn = userUrn;
        }

        @Override
        public void onClick(View v) {
            navigator.openProfile(v.getContext(), userUrn);
        }
    }
}