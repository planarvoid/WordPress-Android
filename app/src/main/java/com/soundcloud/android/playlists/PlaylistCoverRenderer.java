package com.soundcloud.android.playlists;

import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.util.AnimUtils;
import rx.functions.Action0;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

class PlaylistCoverRenderer {

    private final ImageOperations imageOperations;

    @Inject
    PlaylistCoverRenderer(ImageOperations imageOperations) {
        this.imageOperations = imageOperations;
    }

    public void bind(View view, PlaylistDetailsMetadata item, Action0 onHeaderPlay, Action0 onGoToCreator) {
        bindTitle(view, item);
        bindUsername(view, item, onGoToCreator);
        bindArtwork(view, item);
        bindPlayButton(view, item, onHeaderPlay);
    }

    private void bindTitle(View view, PlaylistDetailsMetadata item) {
        final TextView titleView = getTitleView(view, item);
        titleView.setText(item.title());
        titleView.setVisibility(View.VISIBLE);
    }

    private void bindUsername(View view, PlaylistDetailsMetadata item, Action0 onGoToCreator) {
        final TextView usernameView = ButterKnife.findById(view, R.id.username);
        final int proBadge = item.creatorIsPro() ? R.drawable.pro_badge_inset : 0;
        usernameView.setText(item.creatorName());
        usernameView.setCompoundDrawablesWithIntrinsicBounds(0, 0, proBadge, 0);
        usernameView.setEnabled(true);
        usernameView.setOnClickListener(v -> onGoToCreator.call());
        usernameView.setVisibility(View.VISIBLE);
    }

    private void bindArtwork(View view, PlaylistDetailsMetadata item) {
        final ImageView artworkView = ButterKnife.findById(view, R.id.artwork);
        imageOperations.displayWithPlaceholder(item.getUrn(), item.getImageUrlTemplate(), ApiImageSize.getFullImageSize(view.getResources()), artworkView);
    }

    private void bindPlayButton(View view, PlaylistDetailsMetadata item, Action0 onHeaderPlay) {
        final View playButton = ButterKnife.findById(view, R.id.btn_play);
        playButton.setOnClickListener(v -> onHeaderPlay.call());

        if (item.canBePlayed() && !item.isInEditMode()) {
            AnimUtils.showView(playButton, true);
        } else {
            AnimUtils.hideView(playButton, true);
        }
    }

    private TextView getTitleView(View view, PlaylistDetailsMetadata item) {
        return (item != null && item.isPrivate()) ? ButterKnife.findById(view, R.id.title_private) : ButterKnife.findById(view, R.id.title);
    }
}
