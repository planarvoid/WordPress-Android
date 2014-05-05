package com.soundcloud.android.screens.explore;

import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import com.soundcloud.android.R;
import com.soundcloud.android.explore.ExploreTracksCategoryActivity;
import com.soundcloud.android.screens.LegacyPlayerScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.tests.Han;

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

    public LegacyPlayerScreen playTrack(int index) {
        solo.clickOnView(tracksList().getChildAt(index));
        return new LegacyPlayerScreen(solo);
    }

    public void scrollToBottomOfTracksListAndLoadMoreItems() {
        solo.scrollToBottom(tracksList());
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    //TODO: This should be even more generic GV.items()
    private GridView tracksList() {
        return solo.getCurrentGridView();
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
