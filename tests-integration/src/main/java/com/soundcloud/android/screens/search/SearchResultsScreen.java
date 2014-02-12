package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;

import android.widget.ListView;

public class SearchResultsScreen extends Screen {
    private static final Class ACTIVITY = CombinedSearchActivity.class;

    public SearchResultsScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(getActivity());
    }

    private ListView resultsList() {
        return solo.getCurrentListView();
    }

    public int getResultItemCount() {
        return resultsList().getAdapter().getCount();
    }

    public MainScreen pressBack() {
        solo.goBack();
        // A supposition is made that the previous screen was the main screen
        return new MainScreen(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

    @Override
    public boolean isVisible() {
        return waiter.waitForElement(R.id.search_results_container);
    }
}
