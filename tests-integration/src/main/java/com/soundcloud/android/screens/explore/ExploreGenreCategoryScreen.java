package com.soundcloud.android.screens.explore;

import com.soundcloud.android.R;
import com.soundcloud.android.explore.ExploreTracksCategoryActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.VisualPlayerElement;
import com.soundcloud.android.framework.Han;

import android.view.View;
import android.widget.GridView;
import android.widget.TextView;

public class ExploreGenreCategoryScreen extends Screen {
    private static final Class ACTIVITY = ExploreTracksCategoryActivity.class;

    public ExploreGenreCategoryScreen(Han solo) {
        super(solo);
        waiter.waitForActivity(ACTIVITY);
    }

    public int getItemsOnList() {
        return tracksList().getAdapter().getCount();
    }

    public String getTrackTitle(int index) {
        View view = tracksList().getChildAt(index);

        TextView textView = (TextView) view.findViewById(R.id.title);
        return textView.getText().toString();
    }

    public VisualPlayerElement playTrack(int index) {
        testDriver.wrap(tracksList().getChildAt(index)).click();
        return new VisualPlayerElement(testDriver);
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        testDriver.scrollToBottom(tracksList());
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    //TODO: This should be even more generic GV.items()
    private GridView tracksList() {
        return testDriver.getCurrentGridView();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
