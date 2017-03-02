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
        TextView titleView = getTitleView(view, item);
        titleView.setText(item.title());
        titleView.setVisibility(View.VISIBLE);

        TextView usernameView = ButterKnife.findById(view, R.id.username);
        usernameView.setVisibility(View.VISIBLE);
        usernameView.setText(item.creatorName());
        usernameView.setEnabled(true);
        usernameView.setOnClickListener(v -> onGoToCreator.call());

        ImageView artworkView = ButterKnife.findById(view, R.id.artwork);
        imageOperations.displayWithPlaceholder(item, ApiImageSize.getFullImageSize(view.getResources()), artworkView);

        View playButton = ButterKnife.findById(view, R.id.btn_play);
        playButton.setOnClickListener(v -> onHeaderPlay.call());

        if (item.canBePlayed() && !item.isInEditMode()) {
            AnimUtils.showView(playButton, true);
        } else {
            AnimUtils.hideView(playButton, true);
        }
    }

    private TextView getTitleView(View view, PlaylistDetailsMetadata item) {
        if (item != null && item.isPrivate()) {
            return ButterKnife.findById(view, R.id.title_private);
        }
        return ButterKnife.findById(view, R.id.title);
    }
}
