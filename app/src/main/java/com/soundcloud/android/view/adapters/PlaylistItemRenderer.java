package com.soundcloud.android.view.adapters;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playlists.PlaylistItem;
import com.soundcloud.android.playlists.PlaylistItemMenuPresenter;
import com.soundcloud.android.playlists.PromotedPlaylistItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.PromoterClickViewListener;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.EventBus;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlaylistItemRenderer implements CellRenderer<PlaylistItem> {

    private final Resources resources;
    private final ImageOperations imageOperations;
    private final PlaylistItemMenuPresenter playlistItemMenuPresenter;
    private final EventBus eventBus;
    private final ScreenProvider screenProvider;
    private final Navigator navigator;
    private boolean allowOfflineOptions;

    @Inject
    public PlaylistItemRenderer(Resources resources,
                                ImageOperations imageOperations,
                                PlaylistItemMenuPresenter playlistItemMenuPresenter,
                                EventBus eventBus,
                                ScreenProvider screenProvider,
                                Navigator navigator) {

        this.resources = resources;
        this.imageOperations = imageOperations;
        this.playlistItemMenuPresenter = playlistItemMenuPresenter;
        this.eventBus = eventBus;
        this.screenProvider = screenProvider;
        this.navigator = navigator;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_list_item, parent, false);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlaylistItem> playlists) {
        final PlaylistItem playlist = playlists.get(position);
        getTextView(itemView, R.id.list_item_header).setText(playlist.getCreatorName());
        getTextView(itemView, R.id.list_item_subheader).setText(playlist.getTitle());

        showTrackCount(itemView, playlist);
        showReposter(itemView, playlist);
        showAdditionalInformation(itemView, playlist);

        loadArtwork(itemView, playlist);
        setupOverFlow(itemView.findViewById(R.id.overflow_button), playlist);
    }

    public void allowOfflineOptions() {
        this.allowOfflineOptions = true;
    }

    private void setupOverFlow(final View button, final PlaylistItem playlist) {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playlistItemMenuPresenter.show(button, playlist, allowOfflineOptions);
            }
        });
    }

    private void showTrackCount(View itemView, PlaylistItem playlist) {
        final int trackCount = playlist.getTrackCount();
        final String numberOfTracks = resources.getQuantityString(R.plurals.number_of_sounds, trackCount, trackCount);
        getTextView(itemView, R.id.list_item_right_info).setText(numberOfTracks);
    }

    private void showReposter(View itemView, PlaylistItem playlist) {
        final TextView reposterView = getTextView(itemView, R.id.reposter);
        final Optional<String> optionalReposter = playlist.getReposter();
        if (optionalReposter.isPresent()) {
            reposterView.setVisibility(View.VISIBLE);
            reposterView.setText(optionalReposter.get());
        } else {
            reposterView.setVisibility(View.GONE);
        }
    }

    private void showAdditionalInformation(View itemView, PlaylistItem playlist) {
        hideAllAdditionalInformation(itemView);
        if (playlist instanceof PromotedPlaylistItem) {
            showPromotedLabel(itemView, (PromotedPlaylistItem) playlist);
        } else if (isPrivatePlaylist(playlist)) {
            showPrivateIndicator(itemView);
        } else {
            showLikeCount(itemView, playlist);
        }
    }

    private void hideAllAdditionalInformation(View itemView) {
        getTextView(itemView, R.id.list_item_counter).setVisibility(View.GONE);
        getTextView(itemView, R.id.private_indicator).setVisibility(View.GONE);
        unsetPromoterClickable(itemView);
    }

    private void showPromotedLabel(View itemView, PromotedPlaylistItem promoted) {
        if (promoted.hasPromoter()) {
            String label = resources.getString(R.string.promoted_by_label, promoted.getPromoterName().get());
            setPromoterClickable(showPromotedLabel(itemView, label), promoted);
        } else {
            showPromotedLabel(itemView, resources.getString(R.string.promoted_label));
        }
    }

    private TextView showPromotedLabel(View itemView, String label) {
        TextView promoted = getTextView(itemView, R.id.promoted_playlist);
        promoted.setVisibility(View.VISIBLE);
        promoted.setText(label);
        return promoted;
    }

    private void setPromoterClickable(TextView promoter, PromotedPlaylistItem item) {
        ViewUtils.setTouchClickable(promoter, new PromoterClickViewListener(item, eventBus, screenProvider, navigator));
    }

    private void unsetPromoterClickable(View itemView) {
        TextView promoter = getTextView(itemView, R.id.promoted_playlist);
        ViewUtils.unsetTouchClickable(promoter);
        promoter.setVisibility(View.GONE);
    }

    private Boolean isPrivatePlaylist(PlaylistItem playlist) {
        return playlist.isPrivate();
    }

    private void showPrivateIndicator(View itemView) {
        getTextView(itemView, R.id.private_indicator).setVisibility(View.VISIBLE);
    }

    private void loadArtwork(View itemView, PlaylistItem playlist) {
        imageOperations.displayInAdapterView(
                playlist.getEntityUrn(), ApiImageSize.getListItemImageSize(itemView.getContext()),
                (ImageView) itemView.findViewById(R.id.image));
    }

    private void showLikeCount(View itemView, PlaylistItem playlist) {
        final TextView likesCountText = getTextView(itemView, R.id.list_item_counter);
        final int likesCount = playlist.getLikesCount();
        if (hasLike(likesCount)) {
            likesCountText.setVisibility(View.VISIBLE);
            likesCountText.setText(ScTextUtils.formatNumberWithCommas(likesCount));
            final Drawable heartIcon = likesCountText.getCompoundDrawables()[0];
            heartIcon.setLevel(playlist.isLiked() ? 1 : 0);
        }
    }

    private boolean hasLike(int likesCount) {
        return likesCount > 0;
    }

    private TextView getTextView(final View convertView, final int id) {
        return (TextView) convertView.findViewById(id);
    }
}
