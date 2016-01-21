package com.soundcloud.android.screens.discovery;

import com.soundcloud.android.R;
import com.soundcloud.android.framework.Han;
import com.soundcloud.android.framework.viewelements.TextElement;
import com.soundcloud.android.framework.viewelements.ViewElement;
import com.soundcloud.android.framework.with.With;
import com.soundcloud.android.main.MainActivity;
import com.soundcloud.android.screens.Screen;
import com.soundcloud.android.view.SnappedTagView;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.functions.Function;

import java.util.List;

public class DiscoveryScreen extends Screen {

    private static final Class ACTIVITY = MainActivity.class;

    public DiscoveryScreen(Han solo) {
        super(solo);
    }

    private List<ViewElement> recentTags() {
        return testDriver
                .findOnScreenElement(With.id(R.id.recent_tags))
                .findOnScreenElements(With.className(SnappedTagView.class));
    }

    private ViewElement allTags() {
        return testDriver.findOnScreenElement(With.id(R.id.all_tags));
    }

    public boolean isDisplayingTags() {
        return allTags().isVisible();
    }

    public PlaylistResultsScreen clickOnTag(int index) {
        playlistTags().get(index).click();
        return new PlaylistResultsScreen(testDriver);
    }

    public List<ViewElement> playlistTags() {
        waiter.waitForContentAndRetryIfLoadingFailed();
        return allTags().findOnScreenElements(With.className(SnappedTagView.class));
    }

    public List<String> playlistRecentTags() {
        return Lists.transform(recentTags(), new Function<ViewElement, String>() {
            @Override
            public String apply(ViewElement input) {
                return new TextElement(input).getText();
            }
        });
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
