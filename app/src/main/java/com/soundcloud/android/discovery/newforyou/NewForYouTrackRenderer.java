package com.soundcloud.android.discovery.newforyou;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.discovery.newforyou.NewForYouItem.NewForYouTrackItem;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class NewForYouTrackRenderer implements CellRenderer<NewForYouTrackItem> {
    private final TrackItemRenderer trackItemRenderer;
    private final ScreenProvider screenProvider;

    NewForYouTrackRenderer(TrackItemRenderer.Listener listener,
                           @Provided TrackItemRenderer trackItemRenderer,
                           @Provided ScreenProvider screenProvider) {
        this.trackItemRenderer = trackItemRenderer;
        this.screenProvider = screenProvider;

        this.trackItemRenderer.setListener(listener);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<NewForYouTrackItem> items) {
        trackItemRenderer.bindTrackView(items.get(position).track(),
                                        itemView,
                                        position,
                                        Optional.absent(),
                                        Optional.absent());
    }
}
