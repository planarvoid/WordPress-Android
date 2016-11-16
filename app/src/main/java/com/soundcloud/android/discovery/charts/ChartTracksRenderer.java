package com.soundcloud.android.discovery.charts;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.discovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.events.Module;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.functions.Function;
import com.soundcloud.java.optional.Optional;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class ChartTracksRenderer implements CellRenderer<ChartTrackListItem> {

    private final TrackItemRenderer trackItemRenderer;
    private final ScreenProvider screenProvider;

    @Inject
    ChartTracksRenderer(TrackItemRenderer trackItemRenderer, ScreenProvider screenProvider) {
        this.trackItemRenderer = trackItemRenderer;
        this.screenProvider = screenProvider;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(final int position, View itemView, List<ChartTrackListItem> items) {
        final ChartTrackListItem.Track track = (ChartTrackListItem.Track) items.get(position);
        Optional<TrackSourceInfo> trackSourceInfo = track.chartTrackItem.queryUrn().transform(new Function<Urn, TrackSourceInfo>() {
            public TrackSourceInfo apply(Urn queryUrn) {
                TrackSourceInfo info = new TrackSourceInfo(screenProvider.getLastScreenTag(), true);
                final int queryPosition = position - ChartTracksPresenter.NUM_EXTRA_ITEMS;
                info.setQuerySourceInfo(QuerySourceInfo.create(queryPosition, queryUrn));
                return info;
            }
        });
        trackItemRenderer.bindTrackView(track.chartTrackItem, itemView, position, trackSourceInfo,
                                        Optional.<Module>absent());
    }

}
