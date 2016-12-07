package com.soundcloud.android.stream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.view.adapters.CardEngagementsPresenter.CardEngagementClickListener;
import com.soundcloud.android.view.adapters.CardViewHolder;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StreamItemViewHolder implements CardViewHolder {

    @BindView(R.id.user_image) ImageView userImage;
    @BindView(R.id.header_text) TextView headerText;
    @BindView(R.id.reposter) TextView reposter;
    @BindView(R.id.creation_date) TextView createdAt;
    @BindView(R.id.private_indicator) View privateIndicator;
    @BindView(R.id.private_separator) View privateSeparator;

    @BindView(R.id.promoted_item) TextView promotedItem;
    @BindView(R.id.promoter) TextView promoter;

    @BindView(R.id.image) ImageView image;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.creator) TextView creator;

    @BindView(R.id.play_count) TextView playCount;
    @BindView(R.id.duration) TextView duration;
    @BindView(R.id.genre) TextView genre;
    @BindView(R.id.toggle_like) ToggleButton likeButton;
    @BindView(R.id.now_playing) View nowPlaying;
    @BindView(R.id.overflow_button) View overflowButton;

    @Nullable @BindView(R.id.toggle_repost) ToggleButton repostButton;
    @Nullable @BindView(R.id.go_indicator) View goIndicator;

    private OverflowListener overflowListener;
    private CardEngagementClickListener clickListener;

    public StreamItemViewHolder(View view) {
        ButterKnife.bind(this, view);
    }

    @OnClick(R.id.toggle_like)
    public void like() {
        if (clickListener != null) {
            clickListener.onLikeClick(likeButton);
        }
    }

    // yes this @nullable annotation here is required
    @Nullable
    @OnClick(R.id.toggle_repost)
    public void repost() {
        if (clickListener != null) {
            clickListener.onRepostClick(repostButton);
        }
    }

    @OnClick(R.id.overflow_button)
    public void showOverflow() {
        if (overflowListener != null) {
            overflowListener.onOverflow(overflowButton);
        }
    }

    public void setOverflowListener(OverflowListener overflowListener) {
        this.overflowListener = overflowListener;
    }

    public void setEngagementClickListener(CardEngagementClickListener overflowListener) {
        this.clickListener = overflowListener;
    }

    @Override
    public void hideRepostStats() {
        if (repostButton != null) {
            repostButton.setVisibility(View.GONE);
        }
    }

    public void setHeaderText(SpannableString headerString) {
        headerText.setText(headerString);
        headerText.setVisibility(View.VISIBLE);
    }

    public void setRepostHeader(String userName, SpannableString spannableString) {
        headerText.setText(userName);
        headerText.setVisibility(View.VISIBLE);
        reposter.setText(spannableString);
        reposter.setVisibility(View.VISIBLE);
    }

    public void setPromoterHeader(String username, SpannableString spannableString) {
        promoter.setText(username);
        promoter.setVisibility(View.VISIBLE);
        promotedItem.setText(spannableString);
        promotedItem.setVisibility(View.VISIBLE);
    }

    public void setPromotedHeader(SpannableString promoted) {
        promotedItem.setText(promoted);
        promotedItem.setVisibility(View.VISIBLE);
    }

    public void setTitle(String name) {
        title.setText(name);
    }

    public void setArtist(String name) {
        creator.setText(name);
    }

    public void showNowPlaying() {
        nowPlaying.setVisibility(View.VISIBLE);
    }

    public void resetAdditionalInformation() {
        playCount.setVisibility(View.GONE);
        nowPlaying.setVisibility(View.GONE);
        duration.setVisibility(View.GONE);

        if (repostButton != null) {
            repostButton.setVisibility(View.GONE);
        }
    }

    public void showPlayCount(String countString) {
        playCount.setText(countString);
        playCount.setVisibility(View.VISIBLE);
    }

    public ImageView getImage() {
        return image;
    }

    public ImageView getUserImage() {
        userImage.setVisibility(View.VISIBLE);
        return userImage;
    }

    public Context getContext() {
        return title.getContext();
    }

    public void showLikeStats(String likesCount, boolean isUserLike) {
        likeButton.setTextOn(likesCount);
        likeButton.setTextOff(likesCount);
        likeButton.setChecked(isUserLike);
    }

    public void showRepostStats(String repostsCount, boolean isUserReposted) {
        // in some designs repost button is missing
        if (repostButton != null) {
            repostButton.setTextOn(repostsCount);
            repostButton.setTextOff(repostsCount);
            repostButton.setChecked(isUserReposted);
            repostButton.setVisibility(View.VISIBLE);
        }
    }

    public void setCreatedAt(String formattedTime) {
        createdAt.setText(formattedTime);
        createdAt.setVisibility(View.VISIBLE);
    }

    public void togglePrivateIndicator(boolean isPrivate) {
        privateIndicator.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
        privateSeparator.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
    }

    public void resetTierIndicators() {
        safeSetVisibility(goIndicator, View.GONE);
    }

    public void showGoIndicator() {
        safeSetVisibility(goIndicator, View.VISIBLE);
    }

    private void safeSetVisibility(View view, int visibility) {
        if (view != null) {
            view.setVisibility(visibility);
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

    public void setPromoterClickable(View.OnClickListener clickListener) {
        promoter.setOnClickListener(clickListener);
        userImage.setOnClickListener(clickListener);
    }

    public void hideUserImage() {
        userImage.setVisibility(View.GONE);
    }

    public void resetCardView() {
        headerText.setVisibility(View.GONE);
        reposter.setVisibility(View.GONE);
        createdAt.setVisibility(View.GONE);
        promotedItem.setVisibility(View.GONE);
        promoter.setVisibility(View.GONE);
        togglePrivateIndicator(false);

        promoter.setOnClickListener(null);
        userImage.setOnClickListener(null);
        headerText.setOnClickListener(null);
        creator.setOnClickListener(null);
    }

    public void setCreatorClickable(View.OnClickListener clickListener) {
        headerText.setOnClickListener(clickListener);
        userImage.setOnClickListener(clickListener);
    }

    public void setArtistClickable(View.OnClickListener clickListener) {
        creator.setOnClickListener(clickListener);
    }

    public interface OverflowListener {
        void onOverflow(View overflowButton);
    }

}
