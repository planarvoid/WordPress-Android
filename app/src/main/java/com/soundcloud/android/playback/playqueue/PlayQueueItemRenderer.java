package com.soundcloud.android.playback.playqueue;

import com.soundcloud.android.R;
import com.soundcloud.android.image.ApiImageSize;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.playback.PlayQueueManager;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TieredTracks;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemMenuPresenter;
import com.soundcloud.android.utils.ViewUtils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import javax.inject.Inject;
import java.util.List;

public class PlayQueueItemRenderer implements CellRenderer<TrackItem> {

    private static final float ALPHA_DISABLED = 0.5f;
    private static final float ALPHA_ENABLED = 1.0f;
    private static final int FIVE_PIXELS = 5;

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
    public void bindItemView(final int position, View itemView, List<TrackItem> items) {
        final TrackItem trackItem = items.get(position);
        ViewGroup statusPlaceHolder = (ViewGroup) itemView.findViewById(R.id.status_place_holder);
        View textHolder = itemView.findViewById(R.id.text_holder);
        ImageView imageView = (ImageView) itemView.findViewById(R.id.image);
        TextView title = (TextView) itemView.findViewById(R.id.title);
        TextView creator = (TextView) itemView.findViewById(R.id.creator);
        View overFlowButton = itemView.findViewById(R.id.overflow_button);
        title.setText(trackItem.getTitle());
        creator.setText(trackItem.getCreatorName());
        imageOperations.displayInAdapterView(trackItem,
                                             ApiImageSize.getListItemImageSize(itemView.getResources()),
                                             imageView);
        statusPlaceHolder.removeAllViews();
        setListener(position, itemView, trackItem);
        setClickable(itemView, trackItem);
        addStatusLabels(itemView, trackItem, statusPlaceHolder);
        setRepeatAlpha(imageView, textHolder, trackItem);
        setupOverFlow(trackItem, overFlowButton, position);
        setBackground(trackItem, itemView);
    }

    private void setBackground(TrackItem trackItem, View view) {
        if (trackItem.isPlaying()) {
            view.setBackgroundResource(R.drawable.queue_item_playing_background);
        } else {
            view.setBackgroundResource(R.drawable.queue_item_background);
        }
    }

    private void setListener(final int position, View itemView, final TrackItem trackItem) {
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playQueueManager.setCurrentPlayQueueItem(trackItem.getUrn(), position);
            }
        });
    }

    private void setClickable(View itemView, TrackItem trackItem) {
        if (trackItem.isBlocked()) {
            itemView.setClickable(false);
        }
    }

    private void addStatusLabels(View itemView, TrackItem trackItem, ViewGroup statusPlaceHolder) {
        if (trackItem.isPlaying()) {
            statusPlaceHolder.addView(View.inflate(itemView.getContext(), R.layout.playing, null));
        } else if (trackItem.isBlocked()) {
            statusPlaceHolder.addView(View.inflate(itemView.getContext(), R.layout.not_available, null));
        } else if (TieredTracks.isHighTierPreview(trackItem)) {
            statusPlaceHolder.addView(View.inflate(itemView.getContext(), R.layout.preview, null));
        } else if (TieredTracks.isFullHighTierTrack(trackItem)) {
            statusPlaceHolder.addView(View.inflate(itemView.getContext(), R.layout.go_label, null));
        } else if (trackItem.isPrivate()) {
            statusPlaceHolder.addView(View.inflate(itemView.getContext(), R.layout.private_label, null));
        }
    }

    private void setRepeatAlpha(ImageView imageView, View textHolder, TrackItem trackItem) {
        if (trackItem.isInRepeatMode() && !trackItem.isPlaying()) {
            imageView.setAlpha(ALPHA_DISABLED);
            textHolder.setAlpha(ALPHA_DISABLED);
        } else {
            imageView.setAlpha(ALPHA_ENABLED);
            textHolder.setAlpha(ALPHA_ENABLED);
        }
    }

    private void setupOverFlow(final TrackItem track, final View overflowButton, final int position) {
        ViewUtils.extendTouchArea(overflowButton, ViewUtils.dpToPx(overflowButton.getContext(), FIVE_PIXELS));
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                trackItemMenuPresenter.show(ViewUtils.getFragmentActivity(view), view, track, position);
            }
        });
    }

}
