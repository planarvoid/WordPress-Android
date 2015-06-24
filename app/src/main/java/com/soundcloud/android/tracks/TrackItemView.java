package com.soundcloud.android.tracks;

import com.soundcloud.android.R;
import com.soundcloud.android.utils.ViewUtils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;

public class TrackItemView {

    private final ImageView image;
    private final TextView creator;
    private final TextView title;
    private final TextView duration;
    private final TextView playCount;
    private final TextView reposter;
    private final View nowPlaying;
    private final View privateIndicator;
    private final View upsell;
    private final TextView promoted;
    private OverflowListener overflowListener;

    public TrackItemView(View view) {
        creator = (TextView) view.findViewById(R.id.list_item_header);
        title = (TextView) view.findViewById(R.id.list_item_subheader);
        image = (ImageView) view.findViewById(R.id.image);
        duration = (TextView) view.findViewById(R.id.list_item_right_info);
        playCount = (TextView) view.findViewById(R.id.list_item_counter);
        reposter = (TextView) view.findViewById(R.id.reposter);
        nowPlaying = view.findViewById(R.id.now_playing);
        privateIndicator = view.findViewById(R.id.private_indicator);
        upsell = view.findViewById(R.id.upsell);
        promoted = (TextView) view.findViewById(R.id.promoted_track);

        view.findViewById(R.id.overflow_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (overflowListener != null){
                    overflowListener.onOverflow(v);
                }
            }
        });
    }

    public void setOverflowListener(OverflowListener overflowListener) {
        this.overflowListener = overflowListener;
    }

    public void setCreator(String name) {
        creator.setText(name);
    }

    public void setTitle(String name) {
        title.setText(name);
    }

    public void setDuration(String duration) {
        this.duration.setText(duration);
    }

    public void showPromotedTrack(String text) {
        promoted.setVisibility(View.VISIBLE);
        promoted.setText(text);
    }

    public void setPromotedClickable(View.OnClickListener clickListener) {
        promoted.setClickable(true);
        promoted.setOnClickListener(clickListener);
        ViewUtils.extendTouchArea(promoted, 10);
    }

    public void showNowPlaying() {
        nowPlaying.setVisibility(View.VISIBLE);
    }

    public void showPrivateIndicator() {
        privateIndicator.setVisibility(View.VISIBLE);
    }

    public void resetAdditionalInformation() {
        playCount.setVisibility(View.INVISIBLE);
        nowPlaying.setVisibility(View.INVISIBLE);
        privateIndicator.setVisibility(View.GONE);
        upsell.setVisibility(View.INVISIBLE);
        promoted.setVisibility(View.GONE);
        promoted.setClickable(false);
        ViewUtils.clearTouchDelegate(promoted);
        image.setAlpha(1F);
    }

    public void showPlaycount(String text) {
        playCount.setText(text);
        playCount.setVisibility(View.VISIBLE);
    }

    public void showUpsell() {
        upsell.setVisibility(View.VISIBLE);
        image.setAlpha(.5F);
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

    public interface OverflowListener {
        void onOverflow(View overflowButton);
    }

    public static class Factory {
        @Inject
        public Factory() {}

        public View createItemView(ViewGroup parent) {
            final View inflatedView = LayoutInflater.from(parent.getContext()).inflate(R.layout.track_list_item, parent, false);
            inflatedView.setTag(new TrackItemView(inflatedView));
            return inflatedView;
        }
    }
}
