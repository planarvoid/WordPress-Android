package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.elements.ChartsBucketElement;
import com.soundcloud.android.screens.elements.StationsBucketElement;
import com.soundcloud.android.screens.elements.TrackRecommendationsBucketElement;
import com.soundcloud.android.view.SnappedTagView;

import java.util.List;

public class DiscoveryScreen extends Screen {

    private static final Class ACTIVITY = MainActivity.class;

    public DiscoveryScreen(Han solo) {
        super(solo);
    }

    private ViewElement scrollToAllTags() {
        return scrollToItem(With.id(R.id.all_tags));
    }

    private boolean enoughTagsVisible(int count) {
        return testDriver.findOnScreenElements(With.className(SnappedTagView.class)).size() > count;
    }

    public TrackRecommendationsBucketElement trackRecommendationsBucket() {
        return new TrackRecommendationsBucketElement(testDriver,
                                                     scrollToItem(With.id(R.id.track_recommendations_bucket)));
    }

    public ChartsBucketElement chartBucket() {
        scrollToItem(With.id(R.id.charts_bucket));
        return new ChartsBucketElement(testDriver);
    }

    public StationsBucketElement stationsRecommendationsBucket() {
        return new StationsBucketElement(testDriver, scrollToItem(With.id(R.id.stations_pager)));
    }

    public boolean isDisplayingTags() {
        return scrollToAllTags().isOnScreen();
    }

    public String getTagTitle(int index) {
        scrollToPlaylistTags(index);
        return new TextElement(playlistTags().get(index)).getText();
    }

    public PlaylistResultsScreen clickOnTag(int index) {
        scrollToPlaylistTags(index);
        playlistTags().get(index).click();
        return new PlaylistResultsScreen(testDriver);
    }

    private void scrollToPlaylistTags(int index) {
        waiter.waitForContentAndRetryIfLoadingFailed();
        scrollToAllTags();
        while (!enoughTagsVisible(index)) {
            testDriver.scrollDown();
        }
    }

    private List<ViewElement> playlistTags() {
        return scrollToAllTags().findOnScreenElements(With.className(SnappedTagView.class));
    }

    public SearchScreen clickSearch() {
        testDriver.findOnScreenElement(With.id(R.id.search_text)).click();
        return new SearchScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
