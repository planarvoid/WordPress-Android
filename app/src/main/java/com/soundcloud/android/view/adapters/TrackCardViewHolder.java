package com.soundcloud.android.view.adapters;

import static com.soundcloud.android.tracks.TieredTracks.isFullHighTierTrack;
import static com.soundcloud.android.tracks.TieredTracks.isHighTierPreview;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.tracks.TieredTrack;

import android.content.res.Resources;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;


public class TrackCardViewHolder extends RecyclerView.ViewHolder implements CardViewHolder {
    @Bind(R.id.image) ImageView image;
    @Bind(R.id.title) TextView title;
    @Bind(R.id.creator) TextView creator;
    @Bind(R.id.play_count) TextView playCount;

    @Bind(R.id.toggle_like) ToggleButton likeButton;
    @Bind(R.id.now_playing) View nowPlaying;
    @Bind(R.id.overflow_button) View overflowButton;
    @Nullable @Bind(R.id.toggle_repost) ToggleButton repostButton;

    @Nullable @Bind(R.id.go_indicator) View goIndicator;
    @Nullable @Bind(R.id.preview_indicator) View previewIndicator;

    private CardEngagementsPresenter.CardEngagementClickListener clickListener;
    private final ImageOperations imageOperations;
    private final Navigator navigator;
    private final Resources resources;

    public TrackCardViewHolder(View view,
                               ImageOperations imageOperations,
                               Navigator navigator,
                               Resources resources) {
        super(view);
        this.imageOperations = imageOperations;
        this.navigator = navigator;
        this.resources = resources;
        ButterKnife.bind(this, view);
    }

    @Override
    public void showLikeStats(String likesCount, boolean isUserLike) {
        likeButton.setTextOn(likesCount);
        likeButton.setTextOff(likesCount);
        likeButton.setChecked(isUserLike);
    }

    @Override
    public void showRepostStats(String repostsCount, boolean isUserReposted) {
        // in some designs repost button is missing
        if (repostButton != null) {
            repostButton.setTextOn(repostsCount);
            repostButton.setTextOff(repostsCount);
            repostButton.setChecked(isUserReposted);
            repostButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setEngagementClickListener(CardEngagementsPresenter.CardEngagementClickListener cardEngagementClickListener) {
        this.clickListener = cardEngagementClickListener;
    }

    @Override
    public void hideRepostStats() {
        if (repostButton != null) {
            repostButton.setVisibility(View.GONE);
        }
    }

    // @Nullable is required here to avoid crash in landscape mode
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

    public void resetAdditionalInformation() {
        playCount.setVisibility(View.GONE);
        nowPlaying.setVisibility(View.GONE);

        if (repostButton != null) {
            repostButton.setVisibility(View.GONE);
        }
    }

    public void showNowPlaying() {
        nowPlaying.setVisibility(View.VISIBLE);
    }

    public void showPlayCount(String countString) {
        playCount.setText(countString);
        playCount.setVisibility(View.VISIBLE);
    }

    private void loadArtwork(PlayableItem playableItem) {
        imageOperations.displayInAdapterView(
                playableItem, ApiImageSize.getFullImageSize(resources),
                getImage());
    }

    public ImageView getImage() {
        return image;
    }

    public void setTitle(String name) {
        title.setText(name);
    }

    public void setArtist(String name) {
        creator.setText(name);
    }

    public void setArtistClickable(View.OnClickListener clickListener) {
        creator.setOnClickListener(clickListener);
    }

    public void bindArtworkView(PlayableItem playableItem) {
        loadArtwork(playableItem);
        setTitle(playableItem.getTitle());
        setArtist(playableItem.getCreatorName());
        setArtistClickable(new ProfileClickViewListener(playableItem.getCreatorUrn()));
        setupTierIndicator(playableItem);
    }

    private void setupTierIndicator(PlayableItem playableItem) {
        resetTierIndicators();
        if (playableItem instanceof TieredTrack) {
            TieredTrack track = (TieredTrack) playableItem;
            if (isHighTierPreview(track)) {
                safeSetVisibility(previewIndicator, View.VISIBLE);
            } else if (isFullHighTierTrack(track)) {
                safeSetVisibility(goIndicator, View.VISIBLE);
            }
        }
    }

    private void resetTierIndicators() {
        safeSetVisibility(previewIndicator, View.GONE);
        safeSetVisibility(goIndicator, View.GONE);
    }

    private void safeSetVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
        }
    }

    private class ProfileClickViewListener implements View.OnClickListener {

        private final Urn userUrn;

        ProfileClickViewListener(Urn userUrn) {
            this.userUrn = userUrn;
        }

        @Override
        public void onClick(View v) {
            navigator.legacyOpenProfile(v.getContext(), userUrn);
        }
    }
}
