package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.tracks.OverflowMenuOptions.builder;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenElement;
import com.soundcloud.android.events.EventContextMetadata;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;
import com.soundcloud.annotations.VisibleForTesting;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.strings.Strings;

import android.content.res.Resources;
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

    @Inject
    public PlaylistCardRenderer(Resources resources,
                                Navigator navigator, ImageOperations imageOperations,
                                PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                CardEngagementsPresenter cardEngagementsPresenter) {
        this.resources = resources;
        this.navigator = navigator;
        this.imageOperations = imageOperations;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.cardEngagementsPresenter = cardEngagementsPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View inflatedView =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.default_playlist_card, parent, false);
        inflatedView.setTag(new PlaylistViewHolder(inflatedView));
        return inflatedView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlists) {
        bindPlaylistCardView(playlists.get(position), itemView);
    }

    public void bindPlaylistCardView(PlaylistItem playlist, View itemView) {
        PlaylistViewHolder viewHolder = (PlaylistViewHolder) itemView.getTag();

        bindArtworkView(viewHolder, playlist);
        String tracksQuantity = resources.getQuantityString(R.plurals.number_of_tracks, playlist.getTrackCount());
        viewHolder.trackCount.setText(String.valueOf(playlist.getTrackCount()));
        viewHolder.tracksView.setText(tracksQuantity);

        setupEngagementBar(viewHolder, playlist);
    }

    private void setupEngagementBar(PlaylistViewHolder playlistView, final PlaylistItem playlistItem) {
        cardEngagementsPresenter.bind(playlistView, playlistItem, getEventContextMetadata());
        playlistView.tagList.setText(formatTags(playlistItem.getTags()));
        playlistView.tagList.setVisibility(View.VISIBLE);

        playlistView.overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View overflowButton) {
                playlistItemMenuPresenter.show(overflowButton, playlistItem, builder().build());
            }
        });
    }

    private EventContextMetadata getEventContextMetadata() {
        return EventContextMetadata.builder().invokerScreen(ScreenElement.LIST.get())
                .contextScreen(Screen.SEARCH_PLAYLIST_DISCO.get())
                .pageName(Screen.SEARCH_PLAYLIST_DISCO.get())
                .build();
    }

    private void bindArtworkView(PlaylistViewHolder itemView, final PlayableItem playableItem) {
        loadArtwork(itemView, playableItem);
        itemView.title.setText(playableItem.getTitle());
        itemView.creator.setText(playableItem.getCreatorName());
        itemView.creator.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigator.openProfile(v.getContext(), playableItem.getCreatorUrn());
            }
        });
    }

    private void loadArtwork(PlaylistViewHolder itemView, PlayableItem playableItem) {
        imageOperations.displayInAdapterView(
                playableItem.getEntityUrn(), ApiImageSize.getFullImageSize(resources),
                itemView.image);
    }

    private static String formatTags(List<String> tags) {
        if (tags.size() >= 2) {
            return Strings.joinOn(", ").join(Lists.transform(tags.subList(0, 2), new Function<String, String>() {
                @Override
                public String apply(String tag) {
                    return "#" + tag;
                }
            }));
        } else if (tags.size() == 1) {
            return "#" + tags.get(0);
        } else {
            return Strings.EMPTY;
        }
    }

    @VisibleForTesting
    public static class PlaylistViewHolder extends RecyclerView.ViewHolder implements CardViewHolder {

        @Bind(R.id.track_count) TextView trackCount;
        @Bind(R.id.tracks_text) TextView tracksView;
        @Bind(R.id.playlist_additional_info) TextView tagList;

        @Bind(R.id.image) ImageView image;
        @Bind(R.id.title) TextView title;
        @Bind(R.id.creator) TextView creator;

        @Bind(R.id.toggle_like) ToggleButton likeButton;
        @Bind(R.id.overflow_button) View overflowButton;

        @Nullable
        @Bind(R.id.toggle_repost)
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
        public void setEngagementClickListener(CardEngagementClickListener cardEngagementClickListener) {
            this.clickListener = cardEngagementClickListener;
        }

        @Nullable
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
