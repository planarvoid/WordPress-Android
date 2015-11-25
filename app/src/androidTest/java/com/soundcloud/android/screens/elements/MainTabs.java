package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.discovery.DiscoveryScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.YouScreen;

public class MainTabs extends Tabs {

    private enum Tab {
        HOME, DISCOVERY, COLLECTION, YOU
    }

    public MainTabs(Han solo) {
        super(solo);
    }

    public StreamScreen clickHome() {
        getTabAt(Tab.HOME.ordinal()).click();
        return new StreamScreen(testDriver);
    }

    public DiscoveryScreen clickDiscovery() {
        getTabAt(Tab.DISCOVERY.ordinal()).click();
        return new DiscoveryScreen(testDriver);
    }

    public CollectionsScreen clickCollections() {
        getTabAt(Tab.COLLECTION.ordinal()).click();
        return new CollectionsScreen(testDriver);
    }

    public YouScreen clickYou(){
        getTabAt(Tab.YOU.ordinal()).click();
        getTabAt(Tab.YOU.ordinal()).click();
        return new YouScreen(testDriver);
    }
}
