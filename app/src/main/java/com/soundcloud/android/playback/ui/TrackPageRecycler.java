package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.Urn;

import android.view.View;

import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

class TrackPageRecycler {

    private final Map<Urn, View> viewMap;
    private final Deque<View> scrapViews;

    TrackPageRecycler() {
        viewMap = new LinkedHashMap<>(TrackPagerAdapter.TRACK_VIEW_POOL_SIZE);
        scrapViews = new LinkedList<>();
    }

    public boolean isPageForUrn(View trackPage, Urn urn) {
        return trackPage == viewMap.get(urn);
    }

    boolean hasExistingPage(Urn urn){
        return viewMap.containsKey(urn);
    }

    View removePageByUrn(Urn urn) {
        final View view = viewMap.get(urn);
        viewMap.remove(urn);
        return view;
    }

    View getRecycledPage() {
        if (scrapViews.isEmpty()){
            Map.Entry<Urn, View> entry = viewMap.entrySet().iterator().next();
            View view = entry.getValue();
            viewMap.remove(entry.getKey());
            return view;
        } else {
            return scrapViews.pop();
        }
    }

    void recyclePage(Urn urn, View view) {
        viewMap.put(urn, view);
    }

    void addScrapView(View view) {
        scrapViews.push(view);
    }
}
