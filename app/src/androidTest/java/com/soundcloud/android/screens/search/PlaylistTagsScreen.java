package com.soundcloud.android.screens.search;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.screens.PlaylistResultsScreen;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.screens.StreamScreen;
import com.soundcloud.android.search.SearchActivity;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import android.widget.TextView;

import java.util.List;

public class PlaylistTagsScreen extends Screen {

    private static final Class ACTIVITY = SearchActivity.class;

    public PlaylistTagsScreen(Han solo) {
        super(solo);
        waiter.waitForElement(R.id.all_tags);
    }

    public PlaylistResultsScreen clickOnTag(int index) {
        getTags().get(index).click();
        return new PlaylistResultsScreen(testDriver);
    }

    public List<String> getRecentTags() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return getTagStringsFromContainer();
    }

    public boolean isKeyboardShown() {
        return waiter.waitForKeyboardToBeShown();
    }

    public boolean isDisplayingTags() {
        return tagsContainer().isVisible();
    }

    @Override
    public boolean isVisible() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return waiter.waitForElement(R.id.all_tags);
    }

    public boolean scrollDown() {
        return testDriver.scrollDown();
    }

    private List<String> getTagStringsFromContainer() {
        return Lists.transform(getTags(), new Function<ViewElement, String>() {
            @Override
            public String apply(ViewElement input) {
                return new TextElement(input).getText();
            }
        });
    }

    public List<ViewElement> getTags() {
        if (tagsContainer().isVisible()) {
            waiter.waitForContentAndRetryIfLoadingFailed();
            return tagsContainer().findElements(With.className(TextView.class));
        }
        return null;
    }


    public StreamScreen pressBack() {
        testDriver.goBack();
        return new StreamScreen(testDriver);
    }

    private ViewElement tagsContainer() {
        return testDriver.findElement(With.id(R.id.all_tags));
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }

}
