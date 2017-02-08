package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.utils.ViewUtils;

import android.graphics.drawable.AnimationDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class TrackPlayQueueItemRenderer implements CellRenderer<TrackPlayQueueUIItem> {

    interface TrackClickListener {
        void trackClicked(int listPosition);
    }

    private static final int EXTENDED_TOUCH_DP = 6;

    private final ImageOperations imageOperations;
    private final TrackItemMenuPresenter trackItemMenuPresenter;
    private final FeatureFlags flags;

    private TrackClickListener trackClickListener;

    @Inject
    TrackPlayQueueItemRenderer(ImageOperations imageOperations,
                               TrackItemMenuPresenter trackItemMenuPresenter,
                               FeatureFlags flags) {
        this.imageOperations = imageOperations;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.flags = flags;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_track_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackPlayQueueUIItem> items) {
        final TrackPlayQueueUIItem item = items.get(position);
        itemView.setSelected(item.isPlayingOrPaused());
        ViewGroup statusPlaceHolder = (ViewGroup) itemView.findViewById(R.id.status_place_holder);
        View textHolder = itemView.findViewById(R.id.text_holder);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
        TextView title = (TextView) itemView.findViewById(R.id.title);
        TextView creator = (TextView) itemView.findViewById(R.id.creator);
        ImageView overFlowButton = (ImageView) itemView.findViewById(R.id.overflow_button);
        View goIndicator = itemView.findViewById(R.id.go_indicator);

        title.setText(item.getTitle());
        creator.setText(item.getCreator());
        imageOperations.displayInAdapterView(item.getImageResource(),
                                             ApiImageSize.getListItemImageSize(itemView.getResources()),
                                             imageView);
        setGoIndicator(goIndicator, item);
        statusPlaceHolder.removeAllViews();
        setListener(itemView, position);
        setClickable(itemView, item);
        setStatusLabel(statusPlaceHolder, itemView, item);
        setRepeatAlpha(item, imageView, textHolder, goIndicator);
        setupOverFlow(item, overFlowButton, position);
        setTitleColor(item, title);
    }

    private void setGoIndicator(View indicator, TrackPlayQueueUIItem item) {
        indicator.setSelected(flags.isEnabled(Flag.MID_TIER));
        indicator.setVisibility(item.isGoTrack()
                                ? View.VISIBLE
                                : View.GONE);
    }

    private void setTitleColor(TrackPlayQueueUIItem item, TextView titleTextView) {
        titleTextView.setTextColor(item.getTitleTextColor());
    }

    private void setStatusLabel(ViewGroup statusPlaceHolder, View itemView, TrackPlayQueueUIItem trackPlayQueueUIItem) {
        final PlayState playState = trackPlayQueueUIItem.getPlayState();

        if (playState == PlayState.PLAYING) {
            final View view = View.inflate(itemView.getContext(), R.layout.playing, statusPlaceHolder);
            final TextView label = (TextView) view.findViewById(R.id.now_playing);
            final AnimationDrawable drawable = (AnimationDrawable) label.getCompoundDrawables()[0];
            drawable.start();
        } else if (playState == PlayState.PAUSED) {
            View.inflate(itemView.getContext(), R.layout.paused, statusPlaceHolder);
        } else if (trackPlayQueueUIItem.getStatusLabelId() != -1) {
            View.inflate(itemView.getContext(), trackPlayQueueUIItem.getStatusLabelId(), statusPlaceHolder);
        }
    }

    private void setListener(final View itemView, final int position) {
        itemView.setOnClickListener(view -> {
            if (trackClickListener != null) {
                trackClickListener.trackClicked(position);
            }
        });
    }

    private void setClickable(View itemView, TrackPlayQueueUIItem item) {
        if (item.isBlocked()) {
            itemView.setClickable(false);
        }
    }

    private void setRepeatAlpha(TrackPlayQueueUIItem item, ImageView imageView, View textHolder, View goIndicator) {
        float alpha = QueueUtils.getAlpha(item.getRepeatMode(), item.getPlayState());
        imageView.setAlpha(alpha);
        textHolder.setAlpha(alpha);
        goIndicator.setAlpha(alpha);
    }

    static boolean shouldRerender(PlayQueueManager.RepeatMode oldRepeatMode,
                                  PlayQueueManager.RepeatMode newRepeatMode,
                                  PlayState playstate) {
        if (oldRepeatMode == PlayQueueManager.RepeatMode.REPEAT_NONE && newRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ONE) {
            return playstate == PlayState.COMING_UP;
        } else if (oldRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ONE && newRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ALL) {
            return playstate == PlayState.PLAYED || playstate == PlayState.COMING_UP;
        } else if (oldRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ALL && newRepeatMode == PlayQueueManager.RepeatMode.REPEAT_NONE) {
            return playstate == PlayState.PLAYED;
        } else {
            return false;
        }
    }

    private void setupOverFlow(final TrackPlayQueueUIItem item, final ImageView overflowButton, final int position) {
        ViewUtils.extendTouchArea(overflowButton, ViewUtils.dpToPx(overflowButton.getContext(), EXTENDED_TOUCH_DP));
        overflowButton.setSelected(false);
        if (item.getPlayState() == PlayState.COMING_UP) {
            overflowButton.setImageResource(R.drawable.ic_drag_handle_medium_dark_gray_24dp);
            overflowButton.setOnClickListener(null);
        } else {
            overflowButton.setImageResource(R.drawable.playqueue_track_item_overflow);
            overflowButton.setOnClickListener(view -> trackItemMenuPresenter.show(ViewUtils.getFragmentActivity(view),
                                                                                  view,
                                                                                  item.getTrackItem(),
                                                                                  position));
        }
    }

    public void setTrackClickListener(TrackClickListener trackClickListener) {
        this.trackClickListener = trackClickListener;
    }
}
