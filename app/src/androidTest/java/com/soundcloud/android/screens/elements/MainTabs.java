package com.soundcloud.android.screens.elements;

import static com.soundcloud.android.framework.with.With.contentDescription;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.CollectionScreen;
import com.soundcloud.android.screens.StationsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.YouScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;

public class MainTabs extends Tabs {

    public MainTabs(Han solo) {
        super(solo);
    }

    public StreamScreen clickHome() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_home))).click();
        return new StreamScreen(testDriver);
    }

    public StationsScreen clickStationsHome() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_stations))).click();
        return new StationsScreen(testDriver);
    }

    public DiscoveryScreen clickDiscovery() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_discovery))).click();
        return new DiscoveryScreen(testDriver);
    }

    public CollectionScreen clickCollections() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_collection))).click();
        return new CollectionScreen(testDriver);
    }

    public YouScreen clickYou() {
        getTabWith(contentDescription(testDriver.getString(R.string.tab_you))).click();
        return new YouScreen(testDriver);
    }
}
