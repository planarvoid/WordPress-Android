package com.soundcloud.android.stream;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.LinkType;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.image.SimpleImageResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
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
    private final FeatureFlags flags;

    @Inject
    StreamCardViewPresenter(HeaderSpannableBuilder headerSpannableBuilder, EventBus eventBus,
                            ScreenProvider screenProvider, Navigator navigator, Resources resources,
                            ImageOperations imageOperations, FeatureFlags flags) {

        this.headerSpannableBuilder = headerSpannableBuilder;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.flags = flags;
    }

    void bind(StreamItemViewHolder itemView,
              PlayableItem item,
              EventContextMetadata.Builder eventContextMetadataBuilder) {

        bindHeaderView(itemView, item, item.getUrn(), eventContextMetadataBuilder);
        bindArtworkView(itemView, item, eventContextMetadataBuilder);
    }

    private void bindHeaderView(StreamItemViewHolder itemView,
                                PlayableItem playableItem,
                                Urn itemUrn,
                                EventContextMetadata.Builder eventContextMetadataBuilder) {
        itemView.resetCardView();

        final EventContextMetadata eventContextMetadata = eventContextMetadataBuilder.linkType(LinkType.ATTRIBUTOR)
                                                                                     .build();

        if (playableItem instanceof PromotedListItem) {
            showPromoted(itemView,
                         (PromotedListItem) playableItem,
                         playableItem.getPlayableType(),
                         itemUrn,
                         eventContextMetadata);
        } else {
            loadAvatar(itemView,
                       playableItem.getUserUrn(),
                       playableItem.avatarUrlTemplate(),
                       itemUrn,
                       eventContextMetadata);
            setHeaderText(itemView, playableItem);
            showCreatedAt(itemView, playableItem.getCreatedAt());
            itemView.togglePrivateIndicator(playableItem.isPrivate());
        }
    }

    private void bindArtworkView(StreamItemViewHolder itemView,
                                 PlayableItem playableItem,
                                 EventContextMetadata.Builder eventContextMetadataBuilder) {
        loadArtwork(itemView, playableItem);
        itemView.setTitle(playableItem.title());
        itemView.setArtist(playableItem.creatorName());
        itemView.setArtistClickable(new ProfileClickViewListener(
                playableItem.creatorUrn(),
                playableItem.getUrn(),
                eventContextMetadataBuilder.linkType(LinkType.OWNER).build()));

        bindHighTierLabel(itemView, playableItem);
    }

    private void bindHighTierLabel(StreamItemViewHolder item, PlayableItem playableItem) {
        item.resetTierIndicators();
        if (playableItem instanceof TieredTrack) {
            TieredTrack tieredTrack = ((TieredTrack) playableItem);
            if (isFullHighTierTrack(tieredTrack) || isHighTierPreview(tieredTrack)) {
                item.setGoIndicatorSelected(flags.isEnabled(Flag.MID_TIER_ROLLOUT));
                item.showGoIndicator();
            }
        }
    }

    private void loadArtwork(StreamItemViewHolder itemView, PlayableItem playableItem) {
        imageOperations.displayInAdapterView(
                playableItem, ApiImageSize.getFullImageSize(resources),
                itemView.getImage());
    }

    private void setHeaderText(StreamItemViewHolder itemView, PlayableItem playableItem) {
        boolean isRepost = playableItem.reposter().isPresent();
        final String userName = playableItem.reposter().or(playableItem.creatorName());
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

    private void showPromoted(StreamItemViewHolder itemView,
                              PromotedListItem promoted,
                              String playableType,
                              Urn itemUrn,
                              EventContextMetadata eventContextMetadata) {
        if (promoted.hasPromoter()) {
            final String action = resources.getString(R.string.stream_promoted_action);
            loadAvatar(itemView, promoted.getPromoterUrn().get(), promoted.getAvatarUrlTemplate(), itemUrn,
                       eventContextMetadata);
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

    private void loadAvatar(StreamItemViewHolder itemView,
                            Urn userUrn,
                            Optional<String> avatarUrl,
                            Urn itemUrn,
                            EventContextMetadata eventContextMetadata) {
        final ImageResource avatar = SimpleImageResource.create(userUrn, avatarUrl);
        itemView.setCreatorClickable(new ProfileClickViewListener(userUrn, itemUrn, eventContextMetadata));
        imageOperations.displayCircularInAdapterView(
                avatar, ApiImageSize.getListItemImageSize(resources),
                itemView.getUserImage());
    }

    private class ProfileClickViewListener implements View.OnClickListener {

        private final Urn userUrn;
        private final Urn itemUrn;
        private final EventContextMetadata eventContextMetadata;

        ProfileClickViewListener(Urn userUrn, Urn itemUrn, EventContextMetadata eventContextMetadata) {
            this.userUrn = userUrn;
            this.itemUrn = itemUrn;
            this.eventContextMetadata = eventContextMetadata;
        }

        @Override
        public void onClick(View v) {
            navigator.openProfile(v.getContext(), userUrn, UIEvent.fromNavigation(itemUrn, eventContextMetadata));
        }
    }
}
