package com.soundcloud.android.collection;

import com.soundcloud.android.R;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.main.BaseNavigationTarget;

import android.support.v4.app.Fragment;

public class SaveCollectionNavigationTarget extends BaseNavigationTarget {

    public SaveCollectionNavigationTarget() {
        super(R.string.tab_collection, R.drawable.tab_save_collection);
    }

    @Override
    public Fragment createFragment() {
        return new CollectionFragment();
    }

    @Override
    public Screen getScreen() {
        return Screen.COLLECTIONS;
    }

}
