package com.soundcloud.android.playback.playqueue;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.utils.ViewUtils;

import java.util.List;

import javax.inject.Inject;

public class PlayQueueItemRenderer implements CellRenderer<PlayQueueUIItem> {

    public static final float ALPHA_DISABLED = 0.5f;
    public static final float ALPHA_ENABLED = 1.0f;
    private static final int EXTENDED_TOUCH_DP = 6;

    private final ImageOperations imageOperations;
    private final PlayQueueManager playQueueManager;
    private final TrackItemMenuPresenter trackItemMenuPresenter;

    @Inject
    public PlayQueueItemRenderer(ImageOperations imageOperations,
                                 PlayQueueManager playQueueManager,
                                 TrackItemMenuPresenter trackItemMenuPresenter) {
        this.imageOperations = imageOperations;
        this.playQueueManager = playQueueManager;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_track_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<PlayQueueUIItem> items) {
        final PlayQueueUIItem item = items.get(position);
        ViewGroup statusPlaceHolder = (ViewGroup) itemView.findViewById(R.id.status_place_holder);
        View textHolder = itemView.findViewById(R.id.text_holder);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
        TextView title = (TextView) itemView.findViewById(R.id.title);
        TextView creator = (TextView) itemView.findViewById(R.id.creator);
        ImageView overFlowButton = (ImageView) itemView.findViewById(R.id.overflow_button);
        title.setText(item.getTitle());
        creator.setText(item.getCreator());
        imageOperations.displayInAdapterView(item.getImageResource(),
                ApiImageSize.getListItemImageSize(itemView.getResources()),
                imageView);
        statusPlaceHolder.removeAllViews();
        setListener(position, itemView, item);
        setClickable(itemView, item);
        setStatusLabel(statusPlaceHolder, itemView, item);
        setRepeatAlpha(item, imageView, textHolder);
        setupOverFlow(item, overFlowButton, position);
        setBackground(item, itemView);
        setTitleColor(item, title);
    }

    private void setTitleColor(PlayQueueUIItem item, TextView titleTextView) {
        titleTextView.setTextColor(item.getTitleTextColor());
    }

    private void setStatusLabel(ViewGroup statusPlaceHolder,
                                View itemView,
                                PlayQueueUIItem playQueueUIItem) {
        if (playQueueUIItem.isPlaying()) {
            View.inflate(itemView.getContext(), R.layout.playing, statusPlaceHolder);
        } else if (playQueueUIItem.getStatusLabelId() != -1) {
            View.inflate(itemView.getContext(), playQueueUIItem.getStatusLabelId(), statusPlaceHolder);
        }

    }

    private void setBackground(PlayQueueUIItem item, View view) {
        if (item.isPlaying()) {
            view.setBackgroundResource(R.drawable.queue_item_playing_background);
        } else {
            view.setBackgroundResource(R.drawable.queue_item_background);
        }
    }

    private void setListener(final int position, View itemView, final PlayQueueUIItem item) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playQueueManager.setCurrentPlayQueueItem(item.getUrn(), position);
            }
        });
    }

    private void setClickable(View itemView, PlayQueueUIItem item) {
        if (item.isBlocked()) {
            itemView.setClickable(false);
        }
    }

    private void setRepeatAlpha(PlayQueueUIItem item, ImageView imageView, View textHolder) {
        if (item.isInRepeatMode() && !item.isPlaying()) {
            imageView.setAlpha(ALPHA_DISABLED);
            textHolder.setAlpha(ALPHA_DISABLED);
        } else {
            imageView.setAlpha(ALPHA_ENABLED);
            textHolder.setAlpha(ALPHA_ENABLED);
        }
    }

    private void setupOverFlow(final PlayQueueUIItem item, final ImageView overflowButton, final int position) {
        ViewUtils.extendTouchArea(overflowButton, ViewUtils.dpToPx(overflowButton.getContext(), EXTENDED_TOUCH_DP));
        if (item.isDraggable()) {
            overflowButton.setImageResource(R.drawable.drag_handle);
        } else {
            overflowButton.setImageResource(R.drawable.playqueue_track_item_overflow);
            overflowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    trackItemMenuPresenter.show(ViewUtils.getFragmentActivity(view),
                            view,
                            item.getTrackItem(),
                            position);
                }
            });
        }
    }

}
