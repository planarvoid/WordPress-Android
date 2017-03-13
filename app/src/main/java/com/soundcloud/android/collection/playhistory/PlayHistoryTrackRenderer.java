package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.analytics.ScreenProvider;
import com.soundcloud.android.playback.DiscoverySource;
import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.DownloadableTrackItemRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.java.strings.Strings;

import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

class PlayHistoryTrackRenderer implements CellRenderer<PlayHistoryItemTrack> {

    private final DownloadableTrackItemRenderer renderer;
    private final ScreenProvider screenProvider;

    @Inject
    public PlayHistoryTrackRenderer(DownloadableTrackItemRenderer renderer,
                                    ScreenProvider screenProvider) {
        this.renderer = renderer;
        this.screenProvider = screenProvider;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return renderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<PlayHistoryItemTrack> items) {
        PlayHistoryItemTrack track = items.get(position);
        renderer.bindTrackView(track.trackItem(), itemView, position, getTrackSourceInfo(),
                               Optional.absent());
    }

    @NonNull
    private Optional<TrackSourceInfo> getTrackSourceInfo() {
        TrackSourceInfo info = new TrackSourceInfo(screenProvider.getLastScreenTag(), true);
        info.setSource(DiscoverySource.HISTORY.value(), Strings.EMPTY);
        return Optional.of(info);
    }

    public void setListener(TrackItemRenderer.Listener listener) {
        renderer.setListener(listener);
    }
}
