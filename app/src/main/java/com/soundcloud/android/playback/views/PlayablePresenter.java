package com.soundcloud.android.playback.views;

import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.Playable;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.NotNull;

import android.content.res.Resources;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import javax.annotation.Nullable;
import javax.inject.Inject;

public class PlayablePresenter {

    private final ImageOperations imageOperations;
    private final Resources resources;

    @Nullable private TextView titleView;
    @Nullable private TextView usernameView;
    @Nullable private ImageView artworkView;
    @Nullable private StatsView statsView;
    @Nullable private TextView createdAtView;
    @Nullable private TextView privateIndicator;

    private ApiImageSize artworkSize = ApiImageSize.Unknown;

    @Inject
    public PlayablePresenter(ImageOperations imageOperations, Resources resources) {
        this.imageOperations = imageOperations;
        this.resources = resources;
    }

    public PlayablePresenter setTitleView(TextView titleView) {
        this.titleView = titleView;
        return this;
    }

    public PlayablePresenter setUsernameView(TextView usernameView) {
        this.usernameView = usernameView;
        return this;
    }

    public PlayablePresenter setTextVisibility(int visibility) {
        if (titleView != null) {
            titleView.setVisibility(visibility);
        }
        if (usernameView != null) {
            usernameView.setVisibility(visibility);
        }
        return this;
    }

    public PlayablePresenter setArtwork(ImageView artworkView, ApiImageSize artworkSize) {
        this.artworkView = artworkView;
        this.artworkSize = artworkSize;
        return this;
    }

    public PlayablePresenter setStatsView(StatsView statsView) {
        this.statsView = statsView;
        return this;
    }

    public PlayablePresenter setPrivacyIndicatorView(TextView privacyIndicator) {
        privateIndicator = privacyIndicator;
        return this;
    }

    public PlayablePresenter setCreatedAtView(TextView createdAtView) {
        this.createdAtView = createdAtView;
        return this;
    }

    @Deprecated
    public void setPlayable(@NotNull Playable playable) {
        setPlayable(playable.toPropertySet());
    }

    public void setPlayable(@NotNull PropertySet propertySet) {
        setPlayable(new PlayablePresenterItem(propertySet));
    }

    public void setPlayable(@NotNull PlayablePresenterItem item) {
        if (titleView != null) {
            titleView.setText(item.getTitle());
        }

        if (usernameView != null) {
            usernameView.setText(item.getCreatorName());
        }

        if (artworkView != null) {
            imageOperations.displayWithPlaceholder(item.getUrn(), artworkSize, artworkView);
        }

        if (statsView != null) {
            statsView.updateWithPlayable(item);
        }

        if (createdAtView != null) {
            createdAtView.setText(item.getTimeSinceCreated(resources));
        }

        if (privateIndicator != null) {
            setupPrivateIndicator(item);
        }
    }

    private void setupPrivateIndicator(PlayablePresenterItem item) {
        if (privateIndicator == null) return;

        if (item.isPrivate()) {
            privateIndicator.setBackgroundResource(R.drawable.round_rect_orange);
            privateIndicator.setText(R.string.private_indicator);
            privateIndicator.setVisibility(View.VISIBLE);
        } else {
            privateIndicator.setVisibility(View.GONE);
        }
    }

}
