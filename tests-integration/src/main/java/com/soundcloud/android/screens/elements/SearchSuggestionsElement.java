package com.soundcloud.android.screens.elements;

import com.soundcloud.android.R;
import com.soundcloud.android.tests.Han;

public class SearchSuggestionsElement extends Element {

    public SearchSuggestionsElement(Han solo) {
        super(solo);
    }

    @Override
    protected int getRootViewId() {
        return R.id.action_search;
    }

}
