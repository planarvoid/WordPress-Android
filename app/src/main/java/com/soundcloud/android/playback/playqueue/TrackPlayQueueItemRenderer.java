package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageResource;
import com.soundcloud.android.presentation.CellRenderer;
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

    private final ImageOperations imageOperations;
    private final TrackItemMenuPresenter trackItemMenuPresenter;

    private TrackClickListener trackClickListener;

    @Inject
    TrackPlayQueueItemRenderer(ImageOperations imageOperations,
                               TrackItemMenuPresenter trackItemMenuPresenter) {
        this.imageOperations = imageOperations;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_track_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackPlayQueueUIItem> items) {
        final TrackPlayQueueUIItem item = items.get(position);
        itemView.setSelected(item.isPlayingOrPaused());
        ViewGroup statusPlaceHolder = itemView.findViewById(R.id.status_place_holder);
        View textHolder = itemView.findViewById(R.id.text_holder);
        ImageView imageView = itemView.findViewById(R.id.image);
        TextView title = itemView.findViewById(R.id.title);
        TextView creator = itemView.findViewById(R.id.creator);
        ImageView overFlowButton = itemView.findViewById(R.id.overflow_button);
        View goIndicator = itemView.findViewById(R.id.go_indicator);

        title.setText(item.getTitle());
        creator.setText(item.getCreator());
        ImageResource imageResource = item.getImageResource();
        imageOperations.displayInAdapterView(imageResource.getUrn(),
                                             imageResource.getImageUrlTemplate(),
                                             ApiImageSize.getListItemImageSize(itemView.getResources()),
                                             imageView,
                                             false);
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
            final TextView label = view.findViewById(R.id.now_playing);
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

    private void setupOverFlow(final TrackPlayQueueUIItem item, final ImageView overflowButton, final int position) {
        ViewUtils.extendTouchArea(overflowButton);
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
