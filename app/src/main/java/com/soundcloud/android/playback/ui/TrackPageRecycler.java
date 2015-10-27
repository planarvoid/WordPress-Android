package com.soundcloud.android.playback.ui;

import com.soundcloud.android.model.Urn;

import android.support.annotation.Nullable;
import android.view.View;

import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

class TrackPageRecycler {

    private final List<RecycledElement> recycledViews;
    private final Deque<View> scrapViews;

    TrackPageRecycler() {
        recycledViews = new ArrayList<>(PlayerPagerPresenter.PAGE_VIEW_POOL_SIZE);
        scrapViews = new LinkedList<>();
    }

    public boolean isPageForUrn(View trackPage, Urn urn) {
        return trackPage == findView(urn);
    }

    @Nullable
    private View findView(Urn urn) {
        for (int i = 0; i < recycledViews.size(); i++) {
            RecycledElement recycledElement = recycledViews.get(i);
            if (recycledElement.urn.equals(urn)) {
                return recycledElement.view;
            }
        }
        return null;
    }

    boolean hasExistingPage(Urn urn){
        return containsView(urn);
    }

    private boolean containsView(Urn urn) {
        return findView(urn) != null;
    }

    View removePageByUrn(Urn urn) {
        return findAndRemoveView(urn);
    }

    @Nullable
    private View findAndRemoveView(Urn urn) {
        if (!recycledViews.isEmpty()) {
            for (Iterator<RecycledElement> iterator = recycledViews.iterator(); iterator.hasNext(); ) {
                final RecycledElement recycledElement = iterator.next();
                if (recycledElement.urn.equals(urn)) {
                    iterator.remove();
                    return recycledElement.view;
                }
            }
        }
        return null;
    }

    View getRecycledPage() {
        if (scrapViews.isEmpty()){
            return recycledViews.remove(0).view;
        } else {
            return scrapViews.pop();
        }
    }

    void recyclePage(Urn urn, View view) {
        recycledViews.add(new RecycledElement(urn, view));
    }

    void addScrapView(View view) {
        scrapViews.push(view);
    }

    private static class RecycledElement {
        public final Urn urn;
        public final View view;

        private RecycledElement(Urn urn, View view) {
            this.urn = urn;
            this.view = view;
        }
    }
}
