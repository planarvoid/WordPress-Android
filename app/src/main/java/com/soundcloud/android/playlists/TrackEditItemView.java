package com.soundcloud.android.playlists;

import butterknife.BindView;
import butterknife.ButterKnife;
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

class TrackEditItemView {

    @BindView(R.id.image) ImageView image;
    @BindView(R.id.list_item_header) TextView creator;
    @BindView(R.id.list_item_subheader) TextView title;
    @BindView(R.id.list_item_right_info) TextView duration;
    @BindView(R.id.reposter) TextView reposter;
    @BindView(R.id.now_playing) View nowPlaying;
    @BindView(R.id.private_indicator) View privateIndicator;
    @BindView(R.id.promoted_track) TextView promoted;
    @BindView(R.id.not_available_offline) TextView notAvailableOffline;
    @BindView(R.id.preview_indicator) View preview;
    @BindView(R.id.track_list_item_geo_blocked_text) TextView geoBlocked;

    public TrackEditItemView(View rootView) {
        ButterKnife.bind(this, rootView);
    }

    public void setCreator(String name) {
        creator.setText(name);
    }

    public void setTitle(String name, @ColorRes int titleColor) {
        title.setText(name);
        title.setTextColor(getColor(titleColor));
    }

    private int getColor(int color) {
        return getContext().getResources().getColor(color);
    }

    public void showDuration(String duration) {
        this.duration.setText(duration);
        this.duration.setVisibility(View.VISIBLE);
    }

    public void showPromotedTrack(String text) {
        promoted.setVisibility(View.VISIBLE);
        promoted.setText(text);
    }

    public void showGeoBlocked() {
        geoBlocked.setVisibility(View.VISIBLE);
    }

    public void setPromotedClickable(View.OnClickListener clickListener) {
        ViewUtils.setTouchClickable(promoted, clickListener);
    }

    public void showPreviewLabel() {
        preview.setVisibility(View.VISIBLE);
    }

    public void showNowPlaying() {
        nowPlaying.setVisibility(View.VISIBLE);
    }

    public void showPrivateIndicator() {
        privateIndicator.setVisibility(View.VISIBLE);
    }

    public void hideInfoViewsRight() {
        preview.setVisibility(View.GONE);
        privateIndicator.setVisibility(View.GONE);
        duration.setVisibility(View.GONE);
    }

    public void hideInfosViewsBottom() {
        nowPlaying.setVisibility(View.INVISIBLE);
        promoted.setVisibility(View.GONE);
        notAvailableOffline.setVisibility(View.GONE);
        geoBlocked.setVisibility(View.GONE);
        ViewUtils.unsetTouchClickable(promoted);
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

    public void showNotAvailableOffline() {
        notAvailableOffline.setVisibility(View.VISIBLE);
    }

    public interface OverflowListener {
        void onOverflow(View overflowButton);
    }

    static class Factory {
        @Inject
        public Factory() {
        }

        public View createItemView(ViewGroup parent) {
            return createItemView(parent, R.layout.track_list_edit_item);
        }

        public View createItemView(ViewGroup parent, @LayoutRes int layout) {
            final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
            inflatedView.setTag(new TrackEditItemView(inflatedView));
            return inflatedView;
        }
    }
}
