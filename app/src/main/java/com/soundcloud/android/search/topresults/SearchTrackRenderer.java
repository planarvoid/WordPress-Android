package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.java.optional.Optional;
import io.reactivex.subjects.PublishSubject;

import android.view.View;
import android.view.ViewGroup;

import java.util.List;

@AutoFactory
public class SearchTrackRenderer implements CellRenderer<SearchItem.Track> {
    private final TrackItemRenderer trackItemRenderer;
    private final PublishSubject<SearchItem.Track> trackClick;

    SearchTrackRenderer(@Provided TrackItemRenderer trackItemRenderer, PublishSubject<SearchItem.Track> trackClick) {
        this.trackItemRenderer = trackItemRenderer;
        this.trackClick = trackClick;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.Track> items) {
        final SearchItem.Track track = items.get(position);
        itemView.setOnClickListener(view -> trackClick.onNext(track));
        trackItemRenderer.bindTrackView(track.trackItem(), itemView, position, Optional.of(track.trackSourceInfo()), Optional.absent());
    }
}
