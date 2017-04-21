package com.soundcloud.android.olddiscovery.charts;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.olddiscovery.recommendations.QuerySourceInfo;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
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
        Optional<TrackSourceInfo> trackSourceInfo = track.chartTrackItem().queryUrn().transform(queryUrn -> {
            TrackSourceInfo info = new TrackSourceInfo(screenProvider.getLastScreenTag(), true);
            final int queryPosition = position - ChartTracksPresenter.HEADER_OFFSET;
            info.setQuerySourceInfo(QuerySourceInfo.create(queryPosition, queryUrn));
            return info;
        });
        trackItemRenderer.bindChartTrackView(track.chartTrackItem(), itemView, position, trackSourceInfo);
    }
}
