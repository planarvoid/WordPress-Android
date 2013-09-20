package com.soundcloud.android.screens.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

public class ExploreScreen extends Screen {

    private Han solo;

    public ExploreScreen(Han solo) {
        super(solo);
        this.solo = solo;
    }

    public String getActiveTabName() {
        return ((TextView)solo.getView(R.id.nav_explore)).getText().toString();
    }

    public int getItemsOnList() {
        return 0;
    }

    public void scrollDown() {

    }

    public String clickTrack(int i) {
        return "Track name";
    }

    public void clickCategoriesTab() {
        //To change body of created methods use File | Settings | File Templates.
    }

    public boolean hasMusicSection() {
        return true;  //To change body of created methods use File | Settings | File Templates.
    }

    public boolean hasAudioSection() {
        return false;  //To change body of created methods use File | Settings | File Templates.
    }

    public void clickPopularAudioTab() {
        //To change body of created methods use File | Settings | File Templates.
    }

    public void clickCategory(int i) {
        //To change body of created methods use File | Settings | File Templates.
    }
}
