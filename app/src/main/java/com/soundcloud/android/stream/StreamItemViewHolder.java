package com.soundcloud.android.stream;

import butterknife.ButterKnife;
import com.soundcloud.android.R;

import android.content.Context;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class StreamItemViewHolder {

    private final ImageView userImage;
    private final TextView headerText;
    private final TextView reposter;
    private final View privateIndicator;
    private final View privateSeparator;
    private final TextView createdAt;

    private final TextView singleLineHeaderText;
    private final TextView promoter;

    private final ImageView image;
    private final TextView title;
    private final TextView creator;

    private final TextView playCount;
    private final TextView duration;
    private final ToggleButton likeButton;
    private final ToggleButton repostButton;
    private final View nowPlaying;
    private OverflowListener overflowListener;

    public StreamItemViewHolder(View view) {
        userImage = ButterKnife.findById(view, R.id.user_image);
        headerText = ButterKnife.findById(view, R.id.header_text);
        reposter = ButterKnife.findById(view, R.id.reposter);
        createdAt = ButterKnife.findById(view, R.id.creation_date);
        privateIndicator = ButterKnife.findById(view, R.id.private_indicator);
        privateSeparator = ButterKnife.findById(view, R.id.private_separator);

        singleLineHeaderText = ButterKnife.findById(view, R.id.single_line_header_text);
        promoter = ButterKnife.findById(view, R.id.promoter);

        image = ButterKnife.findById(view, R.id.image);
        title = ButterKnife.findById(view, R.id.title);
        creator = ButterKnife.findById(view, R.id.creator);

        nowPlaying = ButterKnife.findById(view, R.id.now_playing);
        playCount = ButterKnife.findById(view, R.id.play_count);
        duration = ButterKnife.findById(view, R.id.playlist_duration);
        likeButton = ButterKnife.findById(view, R.id.toggle_like);
        repostButton = ButterKnife.findById(view, R.id.toggle_repost);

        view.findViewById(R.id.overflow_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (overflowListener != null) {
                    overflowListener.onOverflow(v);
                }
            }
        });
    }

    public void setOverflowListener(OverflowListener overflowListener) {
        this.overflowListener = overflowListener;
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
        singleLineHeaderText.setText(username);
        singleLineHeaderText.setVisibility(View.VISIBLE);
        promoter.setText(spannableString);
        promoter.setVisibility(View.VISIBLE);
    }

    public void setPromotedHeader(SpannableString promoted) {
        promoter.setText(promoted);
        promoter.setVisibility(View.VISIBLE);
    }

    public void setTitle(String name) {
        title.setText(name);
    }

    public void setCreator(String name) {
        creator.setText(name);
    }

    public void showNowPlaying() {
        nowPlaying.setVisibility(View.VISIBLE);
    }

    public void resetAdditionalInformation() {
        playCount.setVisibility(View.GONE);
        nowPlaying.setVisibility(View.GONE);
        duration.setVisibility(View.GONE);
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

    public void resetHeaderView() {
        headerText.setVisibility(View.GONE);
        reposter.setVisibility(View.GONE);
        createdAt.setVisibility(View.GONE);
        singleLineHeaderText.setVisibility(View.GONE);
        promoter.setVisibility(View.GONE);
        togglePrivateIndicator(false);

        singleLineHeaderText.setOnClickListener(null);
        userImage.setOnClickListener(null);
        headerText.setOnClickListener(null);
    }

    public void setCreatorClickable(View.OnClickListener clickListener) {
        headerText.setOnClickListener(clickListener);
        userImage.setOnClickListener(clickListener);
    }

    public interface OverflowListener {
        void onOverflow(View overflowButton);
    }

}
