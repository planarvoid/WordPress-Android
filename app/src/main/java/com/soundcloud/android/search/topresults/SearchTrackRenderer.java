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
import java.util.Map;
import java.util.WeakHashMap;

@AutoFactory
class SearchTrackRenderer implements CellRenderer<SearchItem.Track> {

    private final TrackItemRenderer trackItemRenderer;
    private final PublishSubject<UiAction.TrackClick> trackClick;
    private final Map<View, UiAction.TrackClick> argsMap = new WeakHashMap<>();

    SearchTrackRenderer(@Provided TrackItemRenderer trackItemRenderer,
                        PublishSubject<UiAction.TrackClick> trackClick) {
        this.trackItemRenderer = trackItemRenderer;
        this.trackClick = trackClick;
    }

    @Override
    public View createItemView(ViewGroup parent) {
        final View itemView = trackItemRenderer.createItemView(parent);
        itemView.setOnClickListener(view -> trackClick.onNext(argsMap.get(itemView)));
        return itemView;
    }

    @Override
    public void bindItemView(int position, View itemView, List<SearchItem.Track> items) {
        final SearchItem.Track searchTrack = items.get(position);
        argsMap.put(itemView, searchTrack.clickAction());
        trackItemRenderer.bindTrackView(searchTrack.trackItem(),
                                        itemView, position,
                                        Optional.of(searchTrack.clickAction().trackSourceInfo()),
                                        Optional.of(searchTrack.clickAction().clickParams().get().module()));

    }
}
