package com.soundcloud.android.playback.ui;

import com.soundcloud.android.tracks.TrackUrn;

import android.view.View;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

class TrackPageRecycler {

    private final LinkedHashMap<TrackUrn, View> viewMap;
    private final LinkedList<View> scrapViews;

    TrackPageRecycler() {
        viewMap = new LinkedHashMap<>(TrackPagerAdapter.EXPECTED_TRACKVIEW_COUNT);
        scrapViews = new LinkedList<>();
    }

    boolean hasExistingPage(TrackUrn urn){
        return viewMap.containsKey(urn);
    }

    View getPageByUrn(TrackUrn urn) {
        final View view = viewMap.get(urn);
        viewMap.remove(urn);
        return view;
    }

    View getRecycledPage() {
        if (scrapViews.isEmpty()){
            Map.Entry<TrackUrn, View> entry = viewMap.entrySet().iterator().next();
            View view = entry.getValue();
            viewMap.remove(entry.getKey());
            return view;
        } else {
            return scrapViews.pop();
        }
    }

    void recyclePage(TrackUrn urn, View view) {
        viewMap.put(urn, view);
    }

    void addScrapView(View view) {
        scrapViews.push(view);
    }
}
