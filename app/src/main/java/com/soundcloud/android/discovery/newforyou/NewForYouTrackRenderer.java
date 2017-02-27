package com.soundcloud.android.discovery.newforyou;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.discovery.newforyou.NewForYouItem.NewForYouTrackItem;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory(allowSubclasses = true)
class NewForYouTrackRenderer implements CellRenderer<NewForYouTrackItem> {
    private final TrackItemRenderer trackItemRenderer;

    NewForYouTrackRenderer(TrackItemRenderer.Listener listener,
                           @Provided TrackItemRenderer trackItemRenderer) {
        this.trackItemRenderer = trackItemRenderer;

        this.trackItemRenderer.setListener(listener);
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<NewForYouTrackItem> items) {
        NewForYouTrackItem trackItem = items.get(position);

        TrackSourceInfo info = new TrackSourceInfo(Screen.NEW_FOR_YOU.get(), true);
        info.setSource(DiscoverySource.NEW_FOR_YOU.value(), Strings.EMPTY);
        info.setQuerySourceInfo(QuerySourceInfo.create(position - NewForYouPresenter.NUM_EXTRA_ITEMS, trackItem.newForYou().queryUrn()));

        trackItemRenderer.bindNewForYouTrackView(items.get(position).track(),
                                                 itemView,
                                                 position,
                                                 Optional.of(info));
    }
}
