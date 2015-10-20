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
    private final View privateIndicator;
    private final TextView createdAt;

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
        createdAt = ButterKnife.findById(view, R.id.creation_date);
        privateIndicator = ButterKnife.findById(view, R.id.private_indicator);

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

    public void setHeaderText(SpannableString header) {
        headerText.setText(header);
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

    public void showPlayCount(String playCount) {
        this.playCount.setText(playCount);
        this.playCount.setVisibility(View.VISIBLE);
    }

    public ImageView getImage() {
        return image;
    }

    public ImageView getUserImage() {
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
    }

    public void togglePrivateIndicator(boolean isPrivate) {
        privateIndicator.setVisibility(isPrivate ? View.VISIBLE : View.GONE);
    }

    public void showDuration(String playlistDuration) {
        this.duration.setText(playlistDuration);
        this.duration.setVisibility(View.VISIBLE);
    }

    public interface OverflowListener {
        void onOverflow(View overflowButton);
    }

}
