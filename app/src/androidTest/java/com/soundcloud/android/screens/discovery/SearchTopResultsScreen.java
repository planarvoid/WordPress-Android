package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.discovery.SearchActivity;
import com.soundcloud.android.discovery.SearchPresenter;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.Screen;

public class SearchTopResultsScreen extends Screen {

    /**
     * If the titles for the profile buckets ever change in the resource
     * strings, they will also have to change here. It seems like using
     * resources in tests is a bit unreliable, or doesn't work at all, which
     * is why we have this duplication here.
     */
    public enum Bucket {
        GO_TRACKS("Tracks"),
        TRACKS("Tracks"),
        PEOPLE("People"),
        ALBUMS("Albums"),
        PLAYLISTS("Playlists");

        private final String headerTitle;

        Bucket(String title) {
            this.headerTitle = title;
        }

        public String getHeaderTitle() {
            return this.headerTitle;
        }
    }

    private static final Class ACTIVITY = SearchActivity.class;
    private static final String FRAGMENT = SearchPresenter.SEARCH_RESULTS_TAG;

    public SearchTopResultsScreen(Han solo) {
        super(solo);
        waiter.assertForFragmentByTag(FRAGMENT);
    }

    public ViewElement goTracksHeader() {
        return scrollToItem(With.text(Bucket.GO_TRACKS.getHeaderTitle()));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForFragmentByTag(FRAGMENT);
    }

}
