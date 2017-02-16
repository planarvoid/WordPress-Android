package com.soundcloud.android.search.topresults;

import com.google.auto.factory.AutoFactory;
import com.google.auto.factory.Provided;
import com.soundcloud.android.presentation.CellRenderer;
import com.soundcloud.android.tracks.TrackItemRenderer;
import rx.subjects.PublishSubject;

import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;
import java.util.List;

@AutoFactory
public class SearchTrackRenderer implements CellRenderer<SearchItem.Track> {
    private final TrackItemRenderer trackItemRenderer;
    private final PublishSubject<SearchItem> searchItemClicked;

    @Inject
    public SearchTrackRenderer(@Provided TrackItemRenderer trackItemRenderer, PublishSubject<SearchItem> searchItemClicked) {
        this.trackItemRenderer = trackItemRenderer;
        this.searchItemClicked = searchItemClicked;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        return trackItemRenderer.createItemView(parent);
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.Track> items) {
        final SearchItem.Track track = items.get(position);
        itemView.setOnClickListener(view -> searchItemClicked.onNext(track));
        trackItemRenderer.bindTrackView(position, itemView, track.trackItem());
    }

}
