package com.soundcloud.android.collections;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.BaseNavigationTarget;

import android.support.v4.app.Fragment;

public class CollectionNavigationTarget extends BaseNavigationTarget {

    public CollectionNavigationTarget() {
        super(R.string.tab_collection, R.drawable.tab_collection);
    }

    @Override
    public Fragment createFragment() {
        return new CollectionsFragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.COLLECTIONS;
    }

}
