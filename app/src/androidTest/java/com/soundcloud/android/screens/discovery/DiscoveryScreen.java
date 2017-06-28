package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.MultiContentSelectionCardElement;
import com.soundcloud.android.screens.elements.SingleContentSelectionCardElement;

public class DiscoveryScreen extends Screen {

    private static final Class ACTIVITY = MainActivity.class;

    public DiscoveryScreen(Han solo) {
        super(solo);
    }

    public SearchScreen clickSearch() {
        testDriver.findOnScreenElement(With.id(R.id.search_text)).click();
        return new SearchScreen(testDriver);
    }

    public SingleContentSelectionCardElement singleSelectionCard() {
        final SingleContentSelectionCardElement singleContentSelectionCardElement = new SingleContentSelectionCardElement(testDriver, With.id(R.id.discovery_single_content_selection_card));
        singleContentSelectionCardElement.scrollTo();
        return singleContentSelectionCardElement;
    }

    public MultiContentSelectionCardElement multipleSelectionCard() {
        final MultiContentSelectionCardElement multiContentSelectionCardElement = new MultiContentSelectionCardElement(testDriver, With.id(R.id.discovery_multiple_selection_card));
        multiContentSelectionCardElement.scrollTo();
        return multiContentSelectionCardElement;
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
