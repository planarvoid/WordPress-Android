package com.soundcloud.android.stream;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.soundcloud.android.R;

import android.content.Context;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StreamItemViewHolder {

    @Bind(R.id.user_image) ImageView userImage;
    @Bind(R.id.header_text) TextView headerText;
    @Bind(R.id.reposter) TextView reposter;
    @Bind(R.id.creation_date) TextView createdAt;
    @Bind(R.id.private_indicator) View privateIndicator;
    @Bind(R.id.private_separator) View privateSeparator;

    @Bind(R.id.promoted_item) TextView promotedItem;
    @Bind(R.id.promoter) TextView promoter;

    @Bind(R.id.image) ImageView image;
    @Bind(R.id.title) TextView title;
    @Bind(R.id.creator) TextView creator;

    @Bind(R.id.play_count) TextView playCount;
    @Bind(R.id.playlist_duration) TextView duration;
    @Bind(R.id.toggle_like) ToggleButton likeButton;
    @Bind(R.id.toggle_repost) ToggleButton repostButton;
    @Bind(R.id.now_playing) View nowPlaying;
    @Bind(R.id.overflow_button) View overflowButton;

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
        repostButton.setVisibility(View.GONE);
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
        repostButton.setTextOn(repostsCount);
        repostButton.setTextOff(repostsCount);
        repostButton.setChecked(isUserReposted);
        repostButton.setVisibility(View.VISIBLE);
    }

    public void setCreatedAt(String formattedTime) {
        createdAt.setText(formattedTime);
        createdAt.setVisibility(View.VISIBLE);
    }

    public void togglePrivateIndicator(boolean isPrivate) {
        privateIndicator.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
        privateSeparator.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
    }

    public void showDuration(String playlistDuration) {
        duration.setText(playlistDuration);
        duration.setVisibility(View.VISIBLE);
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

    public interface CardEngagementClickListener {
        void onLikeClick(View likeButton);

        void onRepostClick(View repostButton);
    }
}
