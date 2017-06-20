package com.soundcloud.android.tracks;

import static com.soundcloud.android.utils.ScTextUtils.formatTimeElapsedSince;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.soundcloud.android.R;
import com.soundcloud.android.view.DownloadImageView;
import com.soundcloud.android.offline.OfflineState;
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

    @BindView(R.id.image) ImageView image;
    @BindView(R.id.list_item_header) TextView creator;
    @BindView(R.id.list_item_subheader) TextView title;
    @BindView(R.id.list_item_right_info) TextView duration;
    @BindView(R.id.list_item_counter) TextView playCount;
    @BindView(R.id.reposter) TextView reposter;
    @BindView(R.id.now_playing) View nowPlaying;
    @BindView(R.id.private_indicator) View privateIndicator;
    @BindView(R.id.promoted_track) TextView promoted;
    @BindView(R.id.posted_time) TextView postedTime;
    @BindView(R.id.plays_and_posted_time) TextView playsAndPostedTime;
    @BindView(R.id.preview_indicator) View previewIndicator;
    @BindView(R.id.go_indicator) View goIndicator;
    @BindView(R.id.track_list_item_geo_blocked_text) TextView geoBlocked;
    @BindView(R.id.left_spacer) View leftSpacer;
    @BindView(R.id.position) TextView position;
    @BindView(R.id.overflow_button) View overflowButton;
    @BindView(R.id.track_list_item_offline_state_image_view) DownloadImageView offlineStateImage;
    @BindView(R.id.track_list_item_offline_state_text) TextView offlineStateText;
    @BindView(R.id.track_list_item_no_network_text) TextView noNetwork;

    public TrackItemView(View view) {
        ButterKnife.bind(this, view);
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
        geoBlocked.setVisibility(View.GONE);
        offlineStateImage.setState(OfflineState.NOT_OFFLINE);
        offlineStateText.setVisibility(View.GONE);
        noNetwork.setVisibility(View.GONE);
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

    void showNoWifi() {
        noNetwork.setText(getResources().getString(R.string.offline_no_wifi));
        noNetwork.setVisibility(View.VISIBLE);
    }

    void showNoConnection() {
        noNetwork.setText(getResources().getString(R.string.offline_no_connection));
        noNetwork.setVisibility(View.VISIBLE);
    }

    void showNotAvailableOffline() {
        showOfflineState(OfflineState.UNAVAILABLE, R.string.offline_not_available_offline);
    }

    void showRequested() {
        showOfflineState(OfflineState.REQUESTED, R.string.offline_update_requested);
    }

    void showDownloading() {
        showOfflineState(OfflineState.DOWNLOADING, R.string.offline_update_in_progress);
    }

    void showDownloaded() {
        showOfflineState(OfflineState.DOWNLOADED, R.string.offline_update_completed);
    }

    private void showOfflineState(OfflineState offlineState, int resId) {
        offlineStateImage.setState(offlineState);
        offlineStateText.setText(getResources().getString(resId));
        offlineStateText.setVisibility(View.VISIBLE);
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
