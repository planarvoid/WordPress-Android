package com.soundcloud.android.tracks;

import static com.soundcloud.android.utils.ScTextUtils.formatTimeElapsedSince;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.Date;

public class TrackItemView {

    private final ImageView image;
    private final TextView creator;
    private final TextView title;
    private final TextView duration;
    private final TextView playCount;
    private final TextView reposter;
    private final View nowPlaying;
    private final View privateIndicator;
    private final TextView promoted;
    private final TextView postedTime;
    private final TextView playsAndPostedTime;
    private final TextView notAvailableOffline;
    private final View previewIndicator;
    private final View goIndicator;
    private final TextView geoBlocked;
    private final View leftSpacer;
    private final TextView position;
    private final View overflowButton;

    public TrackItemView(View view) {
        creator = (TextView) view.findViewById(R.id.list_item_header);
        title = (TextView) view.findViewById(R.id.list_item_subheader);
        image = (ImageView) view.findViewById(R.id.image);
        duration = (TextView) view.findViewById(R.id.list_item_right_info);
        playCount = (TextView) view.findViewById(R.id.list_item_counter);
        reposter = (TextView) view.findViewById(R.id.reposter);
        nowPlaying = view.findViewById(R.id.now_playing);
        privateIndicator = view.findViewById(R.id.private_indicator);
        promoted = (TextView) view.findViewById(R.id.promoted_track);
        postedTime = (TextView) view.findViewById(R.id.posted_time);
        playsAndPostedTime = (TextView) view.findViewById(R.id.plays_and_posted_time);
        notAvailableOffline = (TextView) view.findViewById(R.id.not_available_offline);
        previewIndicator = view.findViewById(R.id.preview_indicator);
        goIndicator = view.findViewById(R.id.go_indicator);
        geoBlocked = (TextView) view.findViewById(R.id.track_list_item_geo_blocked_text);
        leftSpacer = view.findViewById(R.id.left_spacer);
        position = (TextView) view.findViewById(R.id.position);
        overflowButton = view.findViewById(R.id.overflow_button);
    }

    void showOverflow(OverflowListener overflowListener) {
        overflowButton.setVisibility(View.VISIBLE);
        setUpOverflowListener(overflowListener);
    }

    private void setUpOverflowListener(OverflowListener overflowListener) {
        overflowButton.setOnClickListener(v -> {
            if (overflowListener != null) {
                overflowListener.onOverflow(v);
            }
        });
    }

    public void setCreator(String name) {
        creator.setText(name);
    }

    public void setTitle(String name, @ColorRes int titleColor) {
        title.setText(name);
        title.setTextColor(getColor(titleColor));
    }

    void showPosition(int index) {
        leftSpacer.setVisibility(View.GONE);
        position.setText(String.valueOf(index));
        position.setVisibility(View.VISIBLE);
    }

    private int getColor(int color) {
        return getContext().getResources().getColor(color);
    }

    void showDuration(String duration) {
        this.duration.setText(duration);
        this.duration.setVisibility(View.VISIBLE);
    }

    public void hideDuration() {
        this.duration.setVisibility(View.GONE);
    }

    void showPromotedTrack(String text) {
        promoted.setVisibility(View.VISIBLE);
        promoted.setText(text);
    }

    void showPostedTime(Date date) {
        postedTime.setVisibility(View.VISIBLE);
        postedTime.setText(getResources().getString(R.string.posted_time,
                                                    formatTimeElapsedSince(getResources(), date.getTime(), true)));
    }

    void showPlaysAndPostedTime(String playCount, Date date) {
        playsAndPostedTime.setVisibility(View.VISIBLE);
        playsAndPostedTime.setText(getResources().getString(R.string.plays_and_posted_time, playCount,
                                                    formatTimeElapsedSince(getResources(), date.getTime(), true)));
    }

    void showGeoBlocked() {
        geoBlocked.setVisibility(View.VISIBLE);
    }

    void setPromotedClickable(View.OnClickListener clickListener) {
        ViewUtils.setTouchClickable(promoted, clickListener);
    }

    void showPreviewLabel() {
        previewIndicator.setVisibility(View.VISIBLE);
    }

    void showGoLabel() {
        goIndicator.setVisibility(View.VISIBLE);
    }

    void showNowPlaying() {
        nowPlaying.setVisibility(View.VISIBLE);
    }

    void showPrivateIndicator() {
        privateIndicator.setVisibility(View.VISIBLE);
    }

    void hideInfoViewsRight() {
        previewIndicator.setVisibility(View.GONE);
        privateIndicator.setVisibility(View.GONE);
        duration.setVisibility(View.GONE);
    }

    void hideInfosViewsBottom() {
        playCount.setVisibility(View.INVISIBLE);
        nowPlaying.setVisibility(View.INVISIBLE);
        promoted.setVisibility(View.GONE);
        postedTime.setVisibility(View.GONE);
        goIndicator.setVisibility(View.GONE);
        playsAndPostedTime.setVisibility(View.GONE);
        notAvailableOffline.setVisibility(View.GONE);
        geoBlocked.setVisibility(View.GONE);
        ViewUtils.unsetTouchClickable(promoted);
    }

    void showPlaycount(String playcount) {
        playCount.setText(playcount);
        playCount.setVisibility(View.VISIBLE);
    }

    public void hideReposter() {
        reposter.setVisibility(View.GONE);
    }

    public void showReposter(String text) {
        reposter.setText(text);
        reposter.setVisibility(View.VISIBLE);
    }

    public ImageView getImage() {
        return image;
    }

    public Context getContext() {
        return title.getContext();
    }

    public Resources getResources() {
        return title.getResources();
    }

    void showNotAvailableOffline() {
        notAvailableOffline.setVisibility(View.VISIBLE);
    }

    interface OverflowListener {
        void onOverflow(View overflowButton);
    }

    public static class Factory {
        private final int disabledTitleColor;
        private final int primaryTitleColor;
        private int layoutId;

        @Inject
        public Factory() {
            this.layoutId = R.layout.track_list_item;
            this.disabledTitleColor = R.color.list_disabled;
            this.primaryTitleColor = R.color.list_primary;
        }


        public void setLayoutId(int layoutId) {
            this.layoutId = layoutId;
        }

        public Factory(@LayoutRes int layoutId, @ColorRes int disabledTitleColor, @ColorRes int primaryTitleColor) {
            this.layoutId = layoutId;
            this.disabledTitleColor = disabledTitleColor;
            this.primaryTitleColor = primaryTitleColor;
        }

        int getDisabledTitleColor() {
            return disabledTitleColor;
        }

        int getPrimaryTitleColor() {
            return primaryTitleColor;
        }

        public View createItemView(ViewGroup parent) {
            return createItemView(parent, layoutId);
        }

        public View createItemView(ViewGroup parent, @LayoutRes int layout) {
            final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            inflatedView.setTag(new TrackItemView(inflatedView));
            return inflatedView;
        }
    }
}
