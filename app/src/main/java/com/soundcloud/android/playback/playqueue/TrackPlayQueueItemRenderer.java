package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.playback.PlaySessionController;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.utils.ViewUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

class TrackPlayQueueItemRenderer implements CellRenderer<TrackPlayQueueUIItem> {

    static final float ALPHA_DISABLED = 0.3f;
    static final float ALPHA_ENABLED = 1.0f;
    private static final int EXTENDED_TOUCH_DP = 6;

    private final ImageOperations imageOperations;
    private final PlayQueueManager playQueueManager;
    private final TrackItemMenuPresenter trackItemMenuPresenter;
    private final PlaySessionController playSessionController;

    @Inject
    TrackPlayQueueItemRenderer(ImageOperations imageOperations,
                                      PlayQueueManager playQueueManager,
                                      TrackItemMenuPresenter trackItemMenuPresenter,
                                      PlaySessionController playSessionController) {
        this.imageOperations = imageOperations;
        this.playQueueManager = playQueueManager;
        this.trackItemMenuPresenter = trackItemMenuPresenter;
        this.playSessionController = playSessionController;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return LayoutInflater.from(parent.getContext()).inflate(R.layout.playqueue_track_item, parent, false);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<TrackPlayQueueUIItem> items) {
        final TrackPlayQueueUIItem item = items.get(position);
        itemView.setSelected(item.getPlayState() == TrackPlayQueueUIItem.PlayState.PLAYING);
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
        setListener(itemView, item);
        setClickable(itemView, item);
        setStatusLabel(statusPlaceHolder, itemView, item);
        setRepeatAlpha(item, imageView, textHolder);
        setupOverFlow(item, overFlowButton, position);
        setTitleColor(item, title);
    }

    private void setTitleColor(TrackPlayQueueUIItem item, TextView titleTextView) {
        titleTextView.setTextColor(item.getTitleTextColor());
    }

    private void setStatusLabel(ViewGroup statusPlaceHolder,
                                View itemView,
                                TrackPlayQueueUIItem trackPlayQueueUIItem) {
        if (trackPlayQueueUIItem.getPlayState() == TrackPlayQueueUIItem.PlayState.PLAYING) {
            View.inflate(itemView.getContext(), R.layout.playing, statusPlaceHolder);
        } else if (trackPlayQueueUIItem.getStatusLabelId() != -1) {
            View.inflate(itemView.getContext(), trackPlayQueueUIItem.getStatusLabelId(), statusPlaceHolder);
        }

    }

    private void setListener(View itemView, final TrackPlayQueueUIItem item) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playQueueManager.setCurrentPlayQueueItem(item.getPlayQueueItem());

                if (!playSessionController.isPlayingCurrentPlayQueueItem()) {
                    playSessionController.play();
                }
            }

        });
    }

    private void setClickable(View itemView, TrackPlayQueueUIItem item) {
        if (item.isBlocked()) {
            itemView.setClickable(false);
        }
    }

    private void setRepeatAlpha(TrackPlayQueueUIItem item, ImageView imageView, View textHolder) {
        float alpha = getAlpha(item.getRepeatMode(), item.getPlayState());
        imageView.setAlpha(alpha);
        textHolder.setAlpha(alpha);
    }

    static float getAlpha(PlayQueueManager.RepeatMode repeatMode, TrackPlayQueueUIItem.PlayState playstate) {
        switch (repeatMode) {
            case REPEAT_NONE:
                if (playstate == TrackPlayQueueUIItem.PlayState.PLAYED) {
                    return ALPHA_DISABLED;
                } else {
                    return ALPHA_ENABLED;
                }
            case REPEAT_ONE:
                if (playstate == TrackPlayQueueUIItem.PlayState.PLAYING) {
                    return ALPHA_ENABLED;
                } else {
                    return ALPHA_DISABLED;
                }
            case REPEAT_ALL:
                return ALPHA_ENABLED;
            default:
                throw new IllegalStateException("Unknown value of repeat mode");
        }
    }

    static boolean shouldRerender(PlayQueueManager.RepeatMode oldRepeatMode,
                                         PlayQueueManager.RepeatMode newRepeatMode,
                                         TrackPlayQueueUIItem.PlayState playstate) {
        if (oldRepeatMode == PlayQueueManager.RepeatMode.REPEAT_NONE && newRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ONE) {
            return playstate == TrackPlayQueueUIItem.PlayState.COMING_UP;
        } else if (oldRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ONE && newRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ALL) {
            return playstate == TrackPlayQueueUIItem.PlayState.PLAYED || playstate == TrackPlayQueueUIItem.PlayState.COMING_UP;
        } else if (oldRepeatMode == PlayQueueManager.RepeatMode.REPEAT_ALL && newRepeatMode == PlayQueueManager.RepeatMode.REPEAT_NONE) {
            return playstate == TrackPlayQueueUIItem.PlayState.PLAYED;
        } else {
            throw new IllegalStateException("New repeat mode: " + newRepeatMode.toString()
                                                    + " cannot follow and old repeat mode: " + oldRepeatMode.toString());
        }
    }

    private void setupOverFlow(final TrackPlayQueueUIItem item, final ImageView overflowButton, final int position) {
        ViewUtils.extendTouchArea(overflowButton, ViewUtils.dpToPx(overflowButton.getContext(), EXTENDED_TOUCH_DP));
        overflowButton.setSelected(false);
        if (item.getPlayState() == TrackPlayQueueUIItem.PlayState.COMING_UP) {
            overflowButton.setImageResource(R.drawable.drag_handle);
            overflowButton.setOnClickListener(null);
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
