package com.soundcloud.android.screens.search;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.search.CombinedSearchActivity;
import com.soundcloud.android.tests.Han;

import android.widget.TextView;

import java.util.List;

public class PlaylistTagsScreen extends Screen {

    private static final Class ACTIVITY = CombinedSearchActivity.class;

    public PlaylistTagsScreen(Han solo) {
        super(solo);
        waiter.waitForContentAndRetryIfLoadingFailed();
    }

    public PlaylistResultsScreen clickOnTag(int index) {
        solo.getSolo().clickOnView(getTagViews(R.id.all_tags).get(index));
        return new PlaylistResultsScreen(solo);
    }

    public List<String> getTags() {
        return getTagStringsFromContainer(R.id.all_tags);
    }

    public List<String> getRecentTags() {
        return getTagStringsFromContainer(R.id.recent_tags);
    }

    public boolean isDisplayingTags() {
        return !getTagViews(R.id.all_tags).isEmpty();
    }

    @Override
    public boolean isVisible() {
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
        return solo.getSolo().getCurrentViews(TextView.class, solo.getView(containerId));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
