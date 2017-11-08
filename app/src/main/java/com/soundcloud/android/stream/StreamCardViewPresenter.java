package com.soundcloud.android.stream;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

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
import com.soundcloud.android.navigation.NavigationTarget;
import com.soundcloud.android.navigation.Navigator;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.view.View;

import javax.inject.Inject;
import java.util.Date;

class StreamCardViewPresenter {

    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Resources resources;
    private final ImageOperations imageOperations;
    private final Navigator navigator;

    @Inject
    StreamCardViewPresenter(EventBus eventBus,
                            ScreenProvider screenProvider,
                            Resources resources,
                            ImageOperations imageOperations,
                            Navigator navigator) {

        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.resources = resources;
        this.imageOperations = imageOperations;
        this.navigator = navigator;
    }

    void bind(StreamItemViewHolder itemView,
              PlayableItem item,
              EventContextMetadata.Builder eventContextMetadataBuilder,
              Date createdAt,
              Optional<String> avatarUrlTemplate) {

        bindHeaderView(itemView, item, item.getUrn(), eventContextMetadataBuilder, createdAt, avatarUrlTemplate);
        bindArtworkView(itemView, item, eventContextMetadataBuilder);
    }

    private void bindHeaderView(StreamItemViewHolder itemView,
                                PlayableItem playableItem,
                                Urn itemUrn,
                                EventContextMetadata.Builder eventContextMetadataBuilder,
                                Date createdAt,
                                Optional<String> avatarUrlTemplate) {
        itemView.resetCardView();

        final EventContextMetadata eventContextMetadata = eventContextMetadataBuilder.linkType(LinkType.ATTRIBUTOR)
                                                                                     .build();

        if (playableItem.isPromoted()) {
            showPromoted(itemView,
                         playableItem,
                         playableItem.getPlayableType(),
                         itemUrn,
                         eventContextMetadata,
                         avatarUrlTemplate);
        } else {
            loadAvatar(itemView,
                       playableItem.getUserUrn(),
                       avatarUrlTemplate,
                       itemUrn,
                       eventContextMetadata);
            setHeaderText(itemView, playableItem);
            showCreatedAt(itemView, createdAt);
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
        if (playableItem instanceof TrackItem) {
            TrackItem tieredTrack = ((TrackItem) playableItem);
            if (isFullHighTierTrack(tieredTrack) || isHighTierPreview(tieredTrack)) {
                item.showGoIndicator();
            }
        }
    }

    private void loadArtwork(StreamItemViewHolder itemView, PlayableItem playableItem) {
        imageOperations.displayInAdapterView(playableItem.getUrn(),
                                             playableItem.getImageUrlTemplate(),
                                             ApiImageSize.getFullImageSize(resources),
                                             itemView.getImage(),
                                             false);
    }

    private void setHeaderText(StreamItemViewHolder itemView, PlayableItem playableItem) {
        boolean isRepost = playableItem.reposter().isPresent();
        final String userName = playableItem.reposter().or(playableItem.creatorName());

        if (isRepost) {
            itemView.setRepostHeader(userName, getRepostMessage(playableItem, Strings.EMPTY));
        } else {
            itemView.setPostHeader(userName, getPostMessage(playableItem, Strings.EMPTY));
        }
    }

    private String getRepostMessage(PlayableItem playableItem, String userName) {
        switch (playableItem.getPlayableType()) {
            case TrackItem.PLAYABLE_TYPE:
                return resources.getString(R.string.stream_reposted_a_track, userName);
            case PlaylistItem.TYPE_PLAYLIST:
                return resources.getString(R.string.stream_reposted_a_playlist, userName);
            case PlaylistItem.TYPE_ALBUM:
                return resources.getString(R.string.stream_reposted_a_album, userName);
            case PlaylistItem.TYPE_EP:
                return resources.getString(R.string.stream_reposted_a_ep, userName);
            case PlaylistItem.TYPE_SINGLE:
                return resources.getString(R.string.stream_reposted_a_single, userName);
            case PlaylistItem.TYPE_COMPILATION:
                return resources.getString(R.string.stream_reposted_a_compilation, userName);
            default:
                return resources.getString(R.string.stream_reposted_a_track, userName);
        }
    }

    private String getPostMessage(PlayableItem playableItem, String userName) {
        switch (playableItem.getPlayableType()) {
            case TrackItem.PLAYABLE_TYPE:
                return resources.getString(R.string.stream_posted_a_track, userName);
            case PlaylistItem.TYPE_PLAYLIST:
                return resources.getString(R.string.stream_posted_a_playlist, userName);
            case PlaylistItem.TYPE_ALBUM:
                return resources.getString(R.string.stream_posted_a_album, userName);
            case PlaylistItem.TYPE_EP:
                return resources.getString(R.string.stream_posted_a_ep, userName);
            case PlaylistItem.TYPE_SINGLE:
                return resources.getString(R.string.stream_posted_a_single, userName);
            case PlaylistItem.TYPE_COMPILATION:
                return resources.getString(R.string.stream_posted_a_compilation, userName);
            default:
                return resources.getString(R.string.stream_posted_a_track, userName);
        }
    }

    private String getPromotedByText(PlayableItem playableItem, String userName) {
        switch (playableItem.getPlayableType()) {
            case TrackItem.PLAYABLE_TYPE:
                return resources.getString(R.string.stream_promoted_a_track, userName);
            case PlaylistItem.TYPE_PLAYLIST:
                return resources.getString(R.string.stream_promoted_a_playlist, userName);
            case PlaylistItem.TYPE_ALBUM:
                return resources.getString(R.string.stream_promoted_a_album, userName);
            case PlaylistItem.TYPE_EP:
                return resources.getString(R.string.stream_promoted_a_ep, userName);
            case PlaylistItem.TYPE_SINGLE:
                return resources.getString(R.string.stream_promoted_a_single, userName);
            case PlaylistItem.TYPE_COMPILATION:
                return resources.getString(R.string.stream_promoted_a_compilation, userName);
            default:
                return resources.getString(R.string.stream_promoted_a_track, userName);
        }
    }

    private String getPromotedText(String playableType) {
        switch (playableType) {
            case TrackItem.PLAYABLE_TYPE:
                return resources.getString(R.string.stream_promoted_track);
            case PlaylistItem.TYPE_PLAYLIST:
                return resources.getString(R.string.stream_promoted_playlist);
            case PlaylistItem.TYPE_ALBUM:
                return resources.getString(R.string.stream_promoted_album);
            case PlaylistItem.TYPE_EP:
                return resources.getString(R.string.stream_promoted_ep);
            case PlaylistItem.TYPE_SINGLE:
                return resources.getString(R.string.stream_promoted_single);
            case PlaylistItem.TYPE_COMPILATION:
                return resources.getString(R.string.stream_promoted_compilation);
            default:
                return resources.getString(R.string.stream_promoted_playlist);
        }
    }

    private void showPromoted(StreamItemViewHolder itemView,
                              PlayableItem promoted,
                              String playableType,
                              Urn itemUrn,
                              EventContextMetadata eventContextMetadata,
                              Optional<String> avatarUrlTemplate) {
        if (promoted.isPromoted() && promoted.promoterUrn().isPresent()) {
            final PromotedProperties promotedProperties = promoted.promotedProperties().get();
            loadAvatar(itemView, promotedProperties.promoterUrn().get(), avatarUrlTemplate, itemUrn, eventContextMetadata);
            String username = promotedProperties.promoterName().get();
            itemView.setPromoterHeader(username, getPromotedByText(promoted, Strings.EMPTY));
            itemView.setPromoterClickable(new PromoterClickViewListener(promoted, eventBus, screenProvider, navigator));
        } else {
            itemView.hideUserImage();
            itemView.setPromotedHeader(getPromotedText(playableType));
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
        imageOperations.displayInAdapterView(avatar.getUrn(),
                                             avatar.getImageUrlTemplate(),
                                             ApiImageSize.getListItemImageSize(resources),
                                             itemView.getUserImage(), true);
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
            navigator.navigateTo(NavigationTarget.forProfile(
                    userUrn,
                    Optional.of(UIEvent.fromNavigation(itemUrn, eventContextMetadata)),
                    Optional.absent(),
                    Optional.absent()));
        }
    }
}
