package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;
import com.soundcloud.android.tests.Waiter;

public class SearchSuggestionsElement {
    protected Han solo;
    protected Waiter waiter;

    private static final int SEARCH_SELECTOR = R.id.action_search;

    public SearchSuggestionsElement(Han solo) {
        this.solo = solo;
        this.waiter = new Waiter(solo);
    }


}
