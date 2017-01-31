package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.with.With.contentDescription;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.MoreScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;

public class MainTabs extends Tabs {

    public MainTabs(Han solo) {
        super(solo);
    }

    public StreamScreen clickHome() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_home))).click();
        return new StreamScreen(testDriver);
    }

    public DiscoveryScreen clickDiscovery() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_discovery))).click();
        return new DiscoveryScreen(testDriver);
    }

    public CollectionScreen clickCollections() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_collection))).click();
        return new CollectionScreen(testDriver);
    }

    public MoreScreen clickMore() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_more))).click();
        return new MoreScreen(testDriver);
    }
}
