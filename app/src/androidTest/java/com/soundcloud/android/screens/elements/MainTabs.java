package com.soundcloud.android.screens.elements;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.CollectionsScreen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.screens.YouScreen;

import android.support.design.widget.TabLayout;

public class MainTabs extends Tabs {

    private final Han testDriver;

    private enum Tab {
        HOME, SEARCH, COLLECTION, YOU
    }

    public MainTabs(Han solo) {
        super(solo.findElement(With.classSimpleName(TabLayout.class.getSimpleName())));
        this.testDriver = solo;
    }

    public StreamScreen clickHome() {
        getTabAt(Tab.HOME.ordinal()).click();
        return new StreamScreen(testDriver);
    }

    public CollectionsScreen clickCollections() {
        getTabAt(Tab.COLLECTION.ordinal()).click();
        return new CollectionsScreen(testDriver);
    }

    public YouScreen clickYou(){
        getTabAt(Tab.YOU.ordinal()).click();
        return new YouScreen(testDriver);
    }
}
