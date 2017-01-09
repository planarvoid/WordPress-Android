package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.tracks.OverflowMenuOptions.builder;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import javax.inject.Inject;
import java.util.List;

public class PlaylistCardRenderer implements CellRenderer<PlaylistItem> {

    private final Resources resources;
    private final Navigator navigator;
    private final ImageOperations imageOperations;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final CardEngagementsPresenter cardEngagementsPresenter;
    private final ScreenProvider screenProvider;
    private int layoutResource = R.layout.default_playlist_card;

    @Inject
    public PlaylistCardRenderer(Resources resources,
                                Navigator navigator, ImageOperations imageOperations,
                                PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                CardEngagementsPresenter cardEngagementsPresenter,
                                ScreenProvider screenProvider) {
        this.resources = resources;
        this.navigator = navigator;
        this.imageOperations = imageOperations;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.cardEngagementsPresenter = cardEngagementsPresenter;
        this.screenProvider = screenProvider;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView =
                LayoutInflater.from(parent.getContext())
                              .inflate(layoutResource, parent, false);
        inflatedView.setTag(new PlaylistViewHolder(inflatedView));
        return inflatedView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlists) {
        bindPlaylistCardView(playlists.get(position), itemView, Optional.<Module>absent());
    }

    public void bindPlaylistCardView(PlaylistItem playlist, View itemView, Optional<Module> module) {
        PlaylistViewHolder viewHolder = (PlaylistViewHolder) itemView.getTag();

        bindArtworkView(viewHolder, playlist);
        String tracksQuantity = resources.getQuantityString(R.plurals.number_of_tracks, playlist.getTrackCount());
        viewHolder.trackCount.setText(String.valueOf(playlist.getTrackCount()));
        viewHolder.tracksView.setText(tracksQuantity);

        setupEngagementBar(viewHolder, playlist, module);
    }

    public void setLayoutResource(@LayoutRes int layoutResource) {
        this.layoutResource = layoutResource;
    }

    private void setupEngagementBar(PlaylistViewHolder playlistView,
                                    final PlaylistItem playlistItem,
                                    final Optional<Module> module) {
        cardEngagementsPresenter.bind(playlistView,
                                      playlistItem,
                                      getEventContextMetadataBuilder(module).build());

        playlistView.overflowButton.setOnClickListener(overflowButton -> playlistItemMenuPresenter.show(overflowButton,
                                                                                                playlistItem,
                                                                                                builder().build(),
                                                                                                getEventContextMetadataBuilder(module)));
    }

    private EventContextMetadata.Builder getEventContextMetadataBuilder(final Optional<Module> module) {
        final EventContextMetadata.Builder builder = EventContextMetadata.builder().invokerScreen(ScreenElement.LIST.get())
                                                                   .contextScreen(screenProvider.getLastScreenTag())
                                                                   .pageName(screenProvider.getLastScreenTag());
        if (module.isPresent()) {
            builder.module(module.get());
        }
        return builder;
    }

    private void bindArtworkView(PlaylistViewHolder itemView, final PlayableItem playableItem) {
        loadArtwork(itemView, playableItem);
        itemView.title.setText(playableItem.getTitle());
        itemView.creator.setText(playableItem.getCreatorName());
        itemView.creator.setOnClickListener(v -> navigator.legacyOpenProfile(v.getContext(), playableItem.getCreatorUrn()));
    }

    private void loadArtwork(PlaylistViewHolder itemView, PlayableItem playableItem) {
        imageOperations.displayInAdapterView(
                playableItem, ApiImageSize.getFullImageSize(resources),
                itemView.image);
    }

    private static String formatTags(List<String> tags) {
        if (tags.size() >= 2) {
            return Strings.joinOn(", ").join(Lists.transform(tags.subList(0, 2), tag -> "#" + tag));
        } else if (tags.size() == 1) {
            return "#" + tags.get(0);
        } else {
            return Strings.EMPTY;
        }
    }

    @VisibleForTesting
    public static class PlaylistViewHolder extends RecyclerView.ViewHolder implements CardViewHolder {

        @BindView(R.id.track_count) TextView trackCount;
        @BindView(R.id.tracks_text) TextView tracksView;
        @BindView(R.id.duration) TextView duration;
        @BindView(R.id.genre) TextView genre;

        @BindView(R.id.image) ImageView image;
        @BindView(R.id.title) TextView title;
        @BindView(R.id.creator) TextView creator;

        @BindView(R.id.toggle_like) ToggleButton likeButton;
        @BindView(R.id.overflow_button) View overflowButton;

        @Nullable
        @BindView(R.id.toggle_repost)
        ToggleButton repostButton;

        private CardEngagementClickListener clickListener;

        public PlaylistViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }

        @Override
        public void showLikeStats(String countString, boolean liked) {
            likeButton.setTextOn(countString);
            likeButton.setTextOff(countString);
            likeButton.setChecked(liked);
        }

        @Override
        public void showRepostStats(String countString, boolean reposted) {
            if (repostButton != null) {
                repostButton.setTextOn(countString);
                repostButton.setTextOff(countString);
                repostButton.setChecked(reposted);
                repostButton.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void showDuration(String duration) {
            this.duration.setText(duration);
            this.duration.setVisibility(View.VISIBLE);
        }

        @Override
        public void showGenre(String genre) {
            this.genre.setText(String.format("#%s", genre));
            this.genre.setVisibility(View.VISIBLE);
        }

        @Override
        public void setEngagementClickListener(CardEngagementClickListener cardEngagementClickListener) {
            this.clickListener = cardEngagementClickListener;
        }

        @Override
        public void hideRepostStats() {
            if (repostButton != null) {
                repostButton.setVisibility(View.GONE);
            }
        }

        @butterknife.Optional
        @OnClick(R.id.toggle_repost)
        public void repost() {
            if (clickListener != null) {
                clickListener.onRepostClick(repostButton);
            }
        }

        @OnClick(R.id.toggle_like)
        public void like() {
            if (clickListener != null) {
                clickListener.onLikeClick(likeButton);
            }
        }

    }

}
