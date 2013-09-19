package com.soundcloud.android.screens.explore;

import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

/**
 * Created with IntelliJ IDEA.
 * User: slawomirsmiechura
 * Date: 9/19/13
 * Time: 1:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class ExploreTracksScreen extends Screen {

    private Han solo;

    public ExploreTracksScreen(Han solo) {
        super(solo);
    }

    public String getTitle() {
        return "Category";
    }

    public int getItemsOnList() {
        return 15;  //To change body of created methods use File | Settings | File Templates.
    }

    public void scrollDown() {
        solo.scrollDown();
    }
}
