package com.soundcloud.android.screens.search;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.MainScreen;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

import java.util.List;

public class PlaylistTagsScreen extends Screen {

    private static final Class ACTIVITY = SearchActivity.class;

    public PlaylistTagsScreen(Han solo) {
        super(solo);
        waiter.waitForElement(R.id.all_tags);
    }

    public PlaylistResultsScreen clickOnTag(int index) {
        testDriver.getSolo().clickOnView(getTagViews(R.id.all_tags).get(index));
        return new PlaylistResultsScreen(testDriver);
    }

    public List<String> getTags() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return getTagStringsFromContainer(R.id.all_tags);
    }

    public List<String> getRecentTags() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return getTagStringsFromContainer(R.id.recent_tags);
    }

    public boolean isKeyboardShown() {
        return waiter.waitForKeyboardToBeShown();
    }

    public boolean isDisplayingTags() {
        return !getTagViews(R.id.all_tags).isEmpty();
    }

    @Override
    public boolean isVisible() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return waiter.waitForElement(R.id.all_tags);
    }

    private List<String> getTagStringsFromContainer(int containerId) {
        return Lists.transform(getTagViews(containerId), new Function<TextView, String>() {
            @Override
            public String apply(TextView input) {
                return input.getText().toString();
            }
        });
    }

    private List<TextView> getTagViews(int containerId) {
        if (testDriver.getView(containerId) != null) {
            waiter.waitForContentAndRetryIfLoadingFailed();
            return testDriver.getSolo().getCurrentViews(TextView.class, testDriver.getView(containerId));
        }
        return null;
    }

    public MainScreen pressBack() {
        testDriver.goBack();
        return new MainScreen(testDriver);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
