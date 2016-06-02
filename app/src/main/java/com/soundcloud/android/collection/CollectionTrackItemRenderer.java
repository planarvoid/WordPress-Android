package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemView;

import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

class CollectionTrackItemRenderer implements CellRenderer<CollectionItem>{

    private final TrackItemRenderer trackItemRenderer;
    private final TrackItemView.Factory trackItemViewFactory;
    private final PlayHistoryOperations playHistoryOperations;
    private final Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;

    @Inject
    public CollectionTrackItemRenderer(TrackItemRenderer trackItemRenderer,
                                       TrackItemView.Factory trackItemViewFactory,
                                       PlayHistoryOperations playHistoryOperations,
                                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        this.trackItemRenderer = trackItemRenderer;
        this.trackItemViewFactory = trackItemViewFactory;
        this.playHistoryOperations = playHistoryOperations;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemViewFactory.createItemView(parent, R.layout.track_list_item_with_divider);
    }

    @Override
    public void bindItemView(int position, View itemView, List<CollectionItem> items) {
        final TrackCollectionItem collectionItem = (TrackCollectionItem) items.get(position);
        final TrackItem trackItem = collectionItem.getTrackItem();

        itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
        trackItemRenderer.bindTrackView(trackItem, itemView, position);
        itemView.setOnClickListener(playTrack(trackItem));
    }

    private View.OnClickListener playTrack(final TrackItem trackItem) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playHistoryOperations.startPlaybackFrom(trackItem.getUrn(), Screen.COLLECTIONS)
                        .subscribe(expandPlayerSubscriberProvider.get());
            }
        };
    }

}
